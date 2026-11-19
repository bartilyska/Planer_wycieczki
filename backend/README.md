# Backend TripPlanner

Java 25, Spring Boot 4.1.2 i Maven 3.6.3 lub nowszy. Backend udostępnia REST przez Spring MVC i komunikuje się z usługami przez WebClient. Harmonogram powstaje dla pojedynczego żądania w pamięci.

## Uruchomienie

Z katalogu `backend`:

```bash
mvn spring-boot:run
```

Tryb integracji domyślnie korzysta z adresów zapisanych w `application.yml`. Planer ma domyślny port `8080`. Zdrowie procesu sprawdzisz pod `http://localhost:8080/actuator/health`. Na komputerze wskazanym w rejestrze adresem sieciowym planera jest `172.20.147.14`. Do uruchomienia z danymi przykładowymi ustaw `PLANNER_DEMO=true`.

Jeśli Atrakcje działają w profilu wymagającym autoryzacji, przed startem ustaw `ATTRACTIONS_TOKEN` na token JWT z zakresem `attractions.read`. Wartością zmiennej jest sam token, bez prefiksu `Bearer`. W profilu `local` Atrakcji token jest opcjonalny.

```bash
mvn clean package
java -jar target/planner-integration-0.1.0-SNAPSHOT.jar
```

`mvn clean package` uruchamia też testy JUnit z mockami Mockito. Sam zestaw można uruchomić przez `mvn test`.

## Zmienne środowiskowe

| Zmienna | Domyślna wartość i zastosowanie |
| --- | --- |
| `PLANNER_DEMO` | `false`. Wartość `true` przełącza wszystkie źródła na jawne dane demonstracyjne. |
| `SERVER_PORT` | `8080` |
| `SERVER_ADDRESS` | `0.0.0.0`. Adres nasłuchu backendu. |
| `FRONTEND_ORIGIN` | `http://localhost:5173,http://172.20.147.14:5173,http://172.20.147.15:5173`. Adresy frontendu dopuszczone przez CORS, oddzielone przecinkami. |
| `ATTRACTIONS_URL` | `http://172.20.147.24:8000` |
| `ATTRACTIONS_TOKEN` | Pusty. Token JWT z zakresem `attractions.read` dla profili wymagających autoryzacji. |
| `TRANSPORT_URL` | `http://172.20.147.17:8083` |
| `TRANSPORT_TIMEOUT` | `2s` na zapytanie do Transportu. |
| `TRANSPORT_LOCATION_ID_MAP` | Pusty. Opcjonalne powiązania `ID_źródłowe=ID_w_Transporcie` oddzielone przecinkami. |
| `GASTRONOMY_URL` | `http://172.20.147.27:8080` |
| `DISCOVERY_ENABLED` | `false`. Włącz po sprawdzeniu integracji pod adresami statycznymi. |
| `DISCOVERY_URL` | `http://172.20.147.20:8080` |
| `ATTRACTIONS_TIMEOUT`, `GASTRONOMY_TIMEOUT` | `5000ms` każda. Timeout pojedynczego wywołania zewnętrznego, łącznie z odczytem odpowiedzi. |

## REST API

| Metoda | Ścieżka | Zastosowanie |
| --- | --- | --- |
| GET | `/api/plans/attractions` | Lista atrakcji. Opcjonalne parametry `city` i `category` filtrują zwrócony katalog. |
| GET | `/api/plans/attractions/{id}` | Szczegóły atrakcji. |
| GET | `/api/plans/options` | Opcje planowania, kategorie, środki transportu i stan konfiguracji usług. |
| POST | `/api/plans/preview` | Harmonogram zachowujący kolejność identyfikatorów z żądania. |
| POST | `/api/plans/optimize` | Harmonogram z heurystycznie zmienioną kolejnością. Wejście identyczne jak dla podglądu. |
| GET | `/actuator/health`, `/actuator/info` | Monitoring procesu. |

Przykład żądania dla trybu demonstracyjnego. Użyj bieżącej lub przyszłej daty w strefie `Europe/Warsaw`.

```bash
curl -sS http://localhost:8080/api/plans/preview \
  -H 'Content-Type: application/json' \
  --data '{
    "city": "Warszawa",
    "date": "2027-06-01",
    "startTime": "09:00",
    "endTime": "18:00",
    "startLocation": {"lat": 52.2297, "lng": 21.0122},
    "attractionIds": ["muzeum-narodowe", "lazienki"],
    "includeGastronomy": true,
    "transportMode": "WALK",
    "returnToStart": true
  }'
```

Plan jest jednodniowy. `endTime` to opcjonalna granica, domyślnie 23:59. Jedno żądanie może zawierać od 1 do 12 różnych atrakcji. Godziny mają dokładność do minuty, a kwoty są całkowitymi liczbami groszy. Optymalizacja stosuje heurystykę zachłanną opartą na czasie przejazdu, odległości w linii prostej do celów oceny kolejności, warunkach pogodowych i dostępnych godzinach otwarcia. Dystans w linii prostej nie jest publikowany jako dystans przejazdu. Godziny otwarcia w modelu wewnętrznym to przedziały przypisane do dnia planu.

Opcjonalne pole `people` przyjmuje wartości od 1 do 20 i domyślnie wynosi 1. Ceny w katalogu dotyczą jednej osoby. Plan używa sumy biletów z propozycji Atrakcji oraz mnoży stawkę Transportu w groszach przez liczbę osób. W trybie integracji środki transportu to `WALK`, `PUBLIC_TRANSPORT` i `CAR`. Atrakcje opisują punkt startowy w pobliżu współrzędnych `52.2297, 21.0122`.

Jeżeli przejazd ma nieznany czas, następne godziny są orientacyjne, a jego `endTime` i `durationMinutes` są puste. Brak ceny lub czasu obniża kompletność sum, co sygnalizują pola `costComplete` i `durationComplete`. Odpowiedź zawiera ostrzeżenia o brakujących integracjach lub danych. Błędy krytyczne przyjmują format Problem Details. HTTP 400 oznacza błędne wejście lub przekroczony przedział harmonogramu, 404 potwierdzony brak atrakcji, 503 problem z obowiązkową integracją, a 500 nieoczekiwany błąd.

## Integracje

| Usługa | Status i punkt rozszerzenia |
| --- | --- |
| Atrakcje | Klient HTTP korzysta z `/api/v1/attractions`, `/api/v1/attractions/{id}`, `/api/v1/categories` i `/api/v1/attractions/proposals`. Plan przyjmuje wyłącznie pozycje dostępne w propozycjach dla wskazanego dnia, startu i środka transportu. |
| Pogoda | Planer mapuje pole `weather` z dziennych propozycji Atrakcji. |
| Transport | `POST /api/v1/routes` podaje czas i koszt odcinków `start` ↔ punkt. Planer może połączyć dwa potwierdzone odcinki przez start i oznacza to w harmonogramie. Transport nie zwraca dystansu ani geometrii. |
| Gastronomia | Po uzgodnieniu protokołu należy dostarczyć komponent `GastronomyClient.Contract`. Adapter mapuje dane na `PlannerSources.Venue` i dostępne godziny otwarcia. |

Po wdrożeniu adaptera we właściwym komponencie i ustawieniu adresu serwisu frontend korzysta z tych samych endpointów planera. Stan `CONFIGURED` sygnalizuje skonfigurowanie adresu i dostępność kodu adaptera. Wynik wywołania konkretnej usługi pojawia się w odpowiedzi katalogu lub w ostrzeżeniach planu.

Planer przekazuje do Transportu `location.id` otrzymane z Atrakcji. Jeśli zespoły używają różnych ID, wpisz w `TRANSPORT_LOCATION_ID_MAP` potwierdzone pary, np. `id-w-atrakcjach=ID_W_TRANSPORCIE`. Lokale będą korzystać z tego mechanizmu, gdy adapter Gastronomii dostarczy ich identyfikatory transportowe. Odpowiedź `NO_CONNECTION` oznacza brak odcinka dla wskazanej pary i trybu. Przy awarii lub timeoutie Planer zgłasza ostrzeżenie i wykorzystuje dostępne czasy oraz koszty z propozycji Atrakcji.

### Discovery

Po sprawdzeniu adresów statycznych uruchom backend poleceniem:

```bash
DISCOVERY_ENABLED=true mvn spring-boot:run
```

Przy starcie w trybie integracji planer wysyła `{"name":"planner"}` na `POST /services`. Discovery odczytuje IP komputera z połączenia. Wyszukiwanie po nazwie ma limit 2 sekund, a wynik jest przechowywany w pamięci przez 10 sekund. Brak wpisu, timeout i awaria rejestru powodują użycie skonfigurowanego adresu. Przy znalezieniu wpisu planer podstawia zwrócone IP i zachowuje port oraz bazową ścieżkę z konfiguracji. Po restarcie rejestru można ponowić rejestrację z komputera planera:

```bash
curl -fsS http://172.20.147.20:8080/services \
  -H 'Content-Type: application/json' --data '{"name":"planner"}'
```

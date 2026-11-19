# Backend TripPlanner

Java 25, Spring Boot 4.1.2 i Maven 3.6.3 lub nowszy. Backend udostępnia REST przez Spring MVC i komunikuje się z usługami przez WebClient. Harmonogram powstaje dla pojedynczego żądania w pamięci.

## Uruchomienie

Z katalogu `backend`:

```bash
mvn spring-boot:run
```

Tryb integracji korzysta z adresów zapisanych w `application.yml`. Przyjęto port `8080` dla wszystkich usług, ponieważ przekazano same adresy IP. Jeśli zespół używa innego portu, nadpisz pełny adres właściwą zmienną środowiskową. Na komputerze planera sprawdzenie procesu jest dostępne pod adresem `http://localhost:8080/actuator/health`, a w sieci lokalnej pod `http://172.20.147.15:8080/actuator/health`. Do uruchomienia z danymi przykładowymi ustaw `PLANNER_DEMO=true`.

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
| `FRONTEND_ORIGIN` | `http://localhost:5173,http://172.20.147.15:5173`. Adresy frontendu dopuszczone przez CORS, oddzielone przecinkami. |
| `ATTRACTIONS_URL` | `http://172.20.147.24:8080` |
| `TRANSPORT_URL` | `http://172.20.147.17:8080` |
| `GASTRONOMY_URL` | `http://172.20.147.27:8080` |
| `ATTRACTIONS_TIMEOUT`, `TRANSPORT_TIMEOUT`, `GASTRONOMY_TIMEOUT` | `5000ms` każda. Timeout pojedynczego wywołania zewnętrznego, łącznie z odczytem odpowiedzi. |

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

Jeżeli przejazd ma nieznany czas, następne godziny są orientacyjne, a jego `endTime` i `durationMinutes` są puste. Brak ceny lub czasu obniża kompletność sum, co sygnalizują pola `costComplete` i `durationComplete`. Odpowiedź zawiera ostrzeżenia o brakujących integracjach lub danych. Błędy krytyczne przyjmują format Problem Details. HTTP 400 oznacza błędne wejście lub przekroczony przedział harmonogramu, 404 potwierdzony brak atrakcji, 503 problem z obowiązkową integracją, a 500 nieoczekiwany błąd.

## Integracje

| Usługa | Status i punkt rozszerzenia |
| --- | --- |
| Atrakcje | Klient HTTP pobiera `/attractions/proposal` i `/attractions/proposal/{id}`. Własne DTO powstaje z `AttractionPayload` przez `AttractionMapper`. Koordynaty są czytane z odpowiedzi szczegółów. Jeśli zespół wydzieli je do osobnego endpointu, klient wymaga uzupełnienia po ustaleniu jego kontraktu. |
| Pogoda | Źródłem prognozy są Atrakcje. Mapowanie w `AttractionsClient` oczekuje na format danych, jaki serwis Atrakcji udostępni dla wybranego dnia. Do tego czasu planer oznacza prognozę jako niedostępną. |
| Transport | Po uzgodnieniu protokołu należy dostarczyć komponent `TransportClient.Contract`. Adapter otrzymuje klienta z dedykowaną konfiguracją HTTP i mapuje odpowiedź do `PlannerSources.Journey`, opcjonalnie z geometrią trasy. |
| Gastronomia | Po uzgodnieniu protokołu należy dostarczyć komponent `GastronomyClient.Contract`. Adapter mapuje dane na `PlannerSources.Venue` i dostępne godziny otwarcia. |

Po wdrożeniu adaptera we właściwym komponencie i ustawieniu adresu serwisu frontend korzysta z tych samych endpointów planera. Stan `CONFIGURED` sygnalizuje skonfigurowanie adresu i dostępność kodu adaptera. Wynik wywołania konkretnej usługi pojawia się w odpowiedzi katalogu lub w ostrzeżeniach planu.

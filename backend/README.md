# Backend TripPlanner

Java 25, Spring Boot 4.1.2 i Maven 3.6.3 lub nowszy. Backend udostępnia REST przez Spring MVC i komunikuje się z usługami przez WebClient. Harmonogram powstaje dla pojedynczego żądania w pamięci.

## Uruchomienie

Z katalogu `backend`:

```bash
PLANNER_DEMO=true mvn spring-boot:run
```

Bez flagi `PLANNER_DEMO` aplikacja działa w trybie integracji. W tym trybie ustaw `ATTRACTIONS_URL` na bazowy adres zespołu Atrakcji, np. przez środowisko procesu. Jeśli adres jest pusty, endpointy wymagające katalogu zwrócą HTTP 503. Sprawdzenie procesu jest dostępne pod adresem `http://localhost:8080/actuator/health`.

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
| `FRONTEND_ORIGIN` | `http://localhost:5173`. Origin dopuszczony przez CORS. |
| `ATTRACTIONS_URL` | Pusty. Bazowy adres katalogu atrakcji. |
| `WEATHER_URL` | Pusty. Bazowy adres usługi prognozy. |
| `TRANSPORT_URL` | Pusty. Bazowy adres usługi przejazdów. |
| `GASTRONOMY_URL` | Pusty. Bazowy adres usługi gastronomicznej. |
| `ATTRACTIONS_TIMEOUT`, `WEATHER_TIMEOUT`, `TRANSPORT_TIMEOUT`, `GASTRONOMY_TIMEOUT` | `5000ms` każda. Timeout pojedynczego wywołania zewnętrznego, łącznie z odczytem odpowiedzi. |

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
| Pogoda | Przygotowano żądanie `GET /api/weather/day?city=…&date=…`. Po uzgodnieniu odpowiedzi należy dostarczyć komponent `WeatherClient.ResponseMapper`, który zwraca `PlanResponse.Weather`. |
| Transport | Po uzgodnieniu protokołu należy dostarczyć komponent `TransportClient.Contract`. Adapter otrzymuje klienta z dedykowaną konfiguracją HTTP i mapuje odpowiedź do `PlannerSources.Journey`, opcjonalnie z geometrią trasy. |
| Gastronomia | Po uzgodnieniu protokołu należy dostarczyć komponent `GastronomyClient.Contract`. Adapter mapuje dane na `PlannerSources.Venue` i dostępne godziny otwarcia. |

Po wdrożeniu adaptera we właściwym komponencie i ustawieniu adresu serwisu frontend korzysta z tych samych endpointów planera. Stan `CONFIGURED` sygnalizuje skonfigurowanie adresu i dostępność kodu adaptera. Wynik wywołania konkretnej usługi pojawia się w odpowiedzi katalogu lub w ostrzeżeniach planu.

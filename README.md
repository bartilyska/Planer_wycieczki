# TripPlanner — Planer i integracja

Planowanie jednodniowych wycieczek na podstawie katalogu atrakcji i dostępnych danych o pogodzie, przejazdach i gastronomii. Plan jest obliczany na żądanie i przechowywany w pamięci bieżącej strony.

## Moduły

- [Backend](backend/README.md) zawiera instrukcje dla Javy 25 i Mavena, opis REST API i konfigurację integracji.
- [Frontend](frontend/README.md) zawiera instrukcje dla Reacta i Vite.

## Szybki start w sieci lokalnej

W pierwszym terminalu:

```bash
cd backend
mvn spring-boot:run
```

W drugim terminalu:

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

Jeśli uruchamiasz oba moduły na komputerze planera, otwórz `http://172.20.147.15:5173`. API planera jest dostępne na porcie `8080`. Adresy Atrakcji, Transportu i Gastronomii zapisano w [konfiguracji backendu](backend/src/main/resources/application.yml). Prognoza będzie pochodzić z Atrakcji po uzgodnieniu formatu danych. Transport i Gastronomia oczekują na kontrakty i adaptery. Do wypróbowania danych przykładowych uruchom backend z `PLANNER_DEMO=true`.

## Specyfikacja projektu

Poniżej znajduje się pierwotna specyfikacja. Aktualną konfigurację adresów i źródło pogody opisuje [dokumentacja backendu](backend/README.md).

### Zadanie implementacyjne

## 1. Cel projektu

Twoim zadaniem jest stworzenie kompletnej aplikacji internetowej realizującej moduł **Planer i integracja**.

Aplikacja ma być centralnym elementem większego systemu tworzonego przez kilka zespołów. Każdy zespół rozwija osobny serwis udostępniający endpointy REST API.

Naszym zadaniem jest stworzenie aplikacji, która komunikuje się z tymi serwisami, pobiera z nich dane, przetwarza je i prezentuje użytkownikowi w postaci przejrzystego, nowoczesnego planera podróży.

Aplikacja ma umożliwiać zaplanowanie dnia w wybranym mieście, uwzględniając:

- dostępne atrakcje turystyczne,
- prognozę pogody,
- transport pomiędzy poszczególnymi punktami,
- propozycje gastronomiczne,
- godziny rozpoczęcia i zakończenia poszczególnych aktywności,
- szacunkowy koszt całego dnia,
- całkowity czas realizacji planu.

Nie twórz wyłącznie demonstracyjnej makiety. Zbuduj rzeczywistą aplikację z działającym backendem, frontendem, komunikacją HTTP, obsługą błędów i przygotowaną architekturą integracyjną.

## 2. Najważniejsze założenia architektoniczne

Projekt musi być podzielony na dwa niezależne katalogi:

- `backend` – Java 25, Spring Boot 4.1, Maven.
- `frontend` – React, Vite, JavaScript lub TypeScript.

Przykładowa struktura głównego katalogu:

```text
planner-integration/
│
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── pl/
│   │   │   │       └── planner/
│   │   │   │           ├── PlannerApplication.java
│   │   │   │           ├── config/
│   │   │   │           ├── controller/
│   │   │   │           ├── service/
│   │   │   │           ├── integration/
│   │   │   │           ├── dto/
│   │   │   │           ├── exception/
│   │   │   │           └── mapper/
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── README.md
│
├── frontend/
│   ├── package.json
│   ├── index.html
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── types/
│   │   ├── styles/
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── README.md
│
├── .gitignore
└── README.md
```

Możesz dostosować strukturę, jeśli istnieje lepsze rozwiązanie, ale zachowaj wyraźny podział odpowiedzialności.

### BARDZO WAŻNE – BRAK WŁASNEJ BAZY DANYCH

Nie twórz żadnej bazy danych.

Nie dodawaj:
- PostgreSQL,
- MySQL,
- MongoDB,
- H2,
- Spring Data JPA,
- Hibernate,
- lokalnych tabel,
- encji bazodanowych,
- migracji Flyway ani Liquibase.

Nie implementuj mechanizmu zapisywania planów do bazy danych.

Dane będą pobierane bezpośrednio z endpointów udostępnianych przez pozostałe zespoły.

Nasza aplikacja ma być warstwą integracyjną i prezentacyjną.

Jeżeli użytkownik utworzy plan, może on istnieć w pamięci aplikacji na czas bieżącego działania lub zostać wygenerowany ponownie na podstawie danych z usług zewnętrznych. Nie implementuj trwałego zapisu bez osobnej decyzji.

Nie twórz sztucznych danych jako domyślnego źródła informacji. Dane demonstracyjne mogą być używane wyłącznie w wyraźnie oznaczonym trybie demonstracyjnym.

## 3. Technologie i konfiguracja backendu

Wykorzystaj:

- Java 25,
- Spring Boot 4.1,
- Maven,
- Spring Web MVC do udostępniania własnego REST API,
- Spring WebFlux WebClient do komunikacji z zewnętrznymi API,
- Spring Boot Validation do walidacji danych,
- Spring Boot Actuator do monitorowania aplikacji,
- JUnit i Mockito do testów - ale nie twórz zbędnych testów.

Skonfiguruj prawidłowy plik `pom.xml`.

Zadbaj o:
- poprawne zależności Spring Boot,
- właściwą wersję Javy,
- konfigurację kompilatora,
- plugin Spring Boot Maven,
- możliwość uruchomienia projektu przez `mvn spring-boot:run`,
- możliwość budowania przez `mvn clean package`.

Nie używaj niepotrzebnych zależności.

Nie mieszaj odpowiedzialności kontrolerów, serwisów i klientów HTTP.

Kontrolery powinny obsługiwać żądania użytkownika, serwisy powinny realizować logikę planowania, a klienci integracyjni powinny komunikować się z serwisami pozostałych zespołów.

## 4. Konfiguracja frontendowa

Wykorzystaj:

- React,
- Vite,
- JavaScript lub TypeScript,
- Axios do komunikacji z backendem,
- Lucide React do ikon,
- CSS lub CSS Modules do stylowania.

Frontend powinien być niezależny od backendu.

Adres backendu konfiguruj przez zmienną środowiskową:

`VITE_API_BASE_URL`

Przykładowy plik `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Nie umieszczaj adresów usług innych zespołów bezpośrednio w frontendzie.

Frontend komunikuje się wyłącznie z naszym backendem. To backend odpowiada za integrację z zewnętrznymi usługami.

## 5. Integracja z pozostałymi zespołami

System musi być przygotowany do integracji z czterema usługami.

### A. Zespół Atrakcje

Udostępniane przykładowe endpointy:

`GET /attractions/proposal`

Zwraca listę dostępnych atrakcji.

Przykładowa odpowiedź:

```json
[
  {
    "id": "muzeum-narodowe",
    "name": "Muzeum Narodowe",
    "location": "Warszawa",
    "category": "kultura",
    "isIndoor": true,
    "priceGroszy": 2000,
    "durationMinutes": 120
  },
  {
    "id": "park-lazienkowski",
    "name": "Park Łazienkowski",
    "location": "Warszawa",
    "category": "przyroda",
    "isIndoor": false,
    "priceGroszy": 0,
    "durationMinutes": 90
  }
]
```

Szczegóły pojedynczej atrakcji:

`GET /attractions/proposal/{id}`

Przykładowa odpowiedź:

```json
{
  "id": "muzeum-narodowe",
  "name": "Muzeum Narodowe",
  "location": "Warszawa",
  "description": "Muzeum z bogatymi zbiorami sztuki.",
  "category": "kultura",
  "isIndoor": true,
  "priceGroszy": 2000,
  "durationMinutes": 120,
  "coordinates": {
    "lat": 52.2319,
    "lng": 21.0222
  }
}
```

Ważne: szczegóły lokalizacji mogą wymagać pobrania osobnego endpointu.

Nasza aplikacja powinna:
- pobierać listę atrakcji,
- umożliwiać filtrowanie po mieście i kategorii, jeśli dane API na to pozwolą,
- umożliwiać wybieranie atrakcji przez użytkownika,
- pobierać szczegóły wybranych atrakcji,
- wykorzystywać koordynaty do komunikacji z transportem.

### B. Zespół Pogoda

Przykładowe endpointy:

`GET /api/weather/day?city=Kraków&date=2026-11-20`

lub:

`GET /api/weather/day?lat=50.0614&lon=19.9366&date=2026-11-20`

Prognoza zakresu:

`GET /api/weather/range?city=Kraków&from=2026-11-20&to=2026-11-22`

Nasza aplikacja potrzebuje:
- temperatury,
- opisu pogody,
- informacji o warunkach atmosferycznych,
- prognozy dla wybranego dnia.

Pogoda powinna wpływać na rekomendowaną kolejność atrakcji.

Przykład:
- deszcz – preferowanie atrakcji krytych,
- dobra pogoda – większy priorytet dla parków i atrakcji zewnętrznych.

Nie zakładaj nieznanej struktury odpowiedzi. Przygotuj DTO i mapper zgodnie z kontraktem, który zostanie przekazany później.

### C. Zespół Transport

Transport udostępni API, które będzie przyjmowało informacje o dwóch punktach i zwracało:

- dystans pomiędzy punktami,
- czas potrzebny na przejazd,
- ewentualnie koszt transportu,
- informacje o wybranym środku transportu, jeśli będzie dostępny.

Dokładne endpointy, parametry i format odpowiedzi zostaną podane później.

Przygotuj osobny `TransportClient` oraz DTO.

Uwzględnij następującą logikę:

Jeżeli użytkownik wybiera kilka atrakcji, transport powinien odbywać się od punktu początkowego do pierwszej atrakcji, następnie do kolejnych atrakcji, a na końcu opcjonalnie z powrotem do punktu początkowego.

Kolejność atrakcji powinna być możliwa do zmiany.

Nie wymyślaj jeszcze kontraktu API transportu.

### D. Zespół Gastronomia

Gastronomia udostępni API zawierające informacje o lokalach gastronomicznych.

Docelowo potrzebujemy:
- nazwy lokalu,
- adresu lub koordynatów,
- kategorii,
- orientacyjnych cen,
- godzin otwarcia, jeśli dostępne,
- ewentualnie czasu potrzebnego na posiłek.

Dokładne endpointy i struktura odpowiedzi zostaną przekazane później.

Przygotuj `GastronomyClient` i miejsce na odpowiednie DTO.

Nie wymyślaj endpointów ani pól, które nie zostały jeszcze uzgodnione.

## 6. Ważne – adresy usług zostaną przekazane później

Na tym etapie nie znamy jeszcze finalnych adresów API pozostałych zespołów.

Nie wpisuj więc na sztywno żadnych docelowych adresów.

Przygotuj konfigurację przez `application.yml` i zmienne środowiskowe.

Przykładowa konfiguracja:

```yaml
server:
  port: 8080

spring:
  application:
    name: planner-integration

integration:
  attractions:
    base-url: ${ATTRACTIONS_URL:}
    timeout: 5000

  weather:
    base-url: ${WEATHER_URL:}
    timeout: 5000

  transport:
    base-url: ${TRANSPORT_URL:}
    timeout: 5000

  gastronomy:
    base-url: ${GASTRONOMY_URL:}
    timeout: 5000
```

Powyższe wartości są celowo puste.

Aplikacja nie powinna próbować wykonywać żądań do nieustawionych adresów.

Jeżeli adres konkretnej usługi nie został skonfigurowany, backend powinien zwrócić kontrolowaną informację o niedostępności integracji, a nie nieczytelny błąd techniczny.

Każda usługa powinna mieć osobną konfigurację, timeout i obsługę błędów.

Po otrzymaniu prawdziwych adresów powinna wystarczyć zmiana konfiguracji bez konieczności przepisywania kodu.

## 7. Własne REST API planera

Przygotuj następujące endpointy.

### GET /api/plans/attractions

Pobieranie listy atrakcji z zewnętrznego serwisu.

### GET /api/plans/attractions/{id}

Pobieranie szczegółów wybranej atrakcji.

### POST /api/plans/preview

Generowanie planu podróży bez zapisywania go w bazie.

Przykładowe żądanie:

```json
{
  "city": "Warszawa",
  "date": "2026-11-20",
  "startTime": "09:00",
  "startLocation": {
    "lat": 52.2297,
    "lng": 21.0122
  },
  "attractionIds": [
    "muzeum-narodowe",
    "park-lazienkowski"
  ],
  "includeGastronomy": true,
  "transportMode": "CAR"
}
```

### POST /api/plans/optimize

Optymalizacja kolejności wybranych atrakcji na podstawie czasu przejazdu, lokalizacji i warunków pogodowych.

Jeżeli ten endpoint dubluje logikę `/preview`, możesz zrealizować optymalizację jako osobny etap logiki serwisowej, ale zachowaj możliwość późniejszego rozdzielenia funkcjonalności.

### GET /api/plans/options

Zwracanie dostępnych opcji planowania, kategorii, środków transportu i ustawień.

Nie implementuj endpointów zapisujących i pobierających trwałe plany, ponieważ nie posiadamy własnej bazy danych.

## 8. Model danych odpowiedzi planera

Zaprojektuj własne DTO niezależne od formatów odpowiedzi innych zespołów.

Dzięki temu zmiana kontraktu zewnętrznego API nie będzie wymagała przebudowy całego frontendu.

Docelowy model odpowiedzi:

```json
{
  "city": "Warszawa",
  "date": "2026-11-20",
  "weather": {
    "temperature": 12,
    "description": "Pochmurno",
    "available": true
  },
  "totalCostGroszy": 4500,
  "totalDurationMinutes": 360,
  "items": [
    {
      "id": "muzeum-narodowe",
      "type": "ATTRACTION",
      "name": "Muzeum Narodowe",
      "startTime": "11:00",
      "endTime": "13:00",
      "durationMinutes": 120,
      "priceGroszy": 2000,
      "distanceKm": null,
      "travelMinutes": null,
      "location": "Warszawa"
    },
    {
      "id": "transport-1",
      "type": "TRANSPORT",
      "name": "Przejazd do Parku Łazienkowskiego",
      "startTime": "13:00",
      "endTime": "13:20",
      "durationMinutes": 20,
      "priceGroszy": 0,
      "distanceKm": 5.2,
      "travelMinutes": 20
    }
  ],
  "warnings": []
}
```

To jest wewnętrzny kontrakt naszej aplikacji, który można rozwijać.

Wszystkie ceny przechowuj i obliczaj w groszach, aby uniknąć błędów zmiennoprzecinkowych.

## 9. Algorytm generowania planu

Zaimplementuj logikę w osobnej klasie serwisowej, a nie w kontrolerze.

Planer powinien wykonywać następujące operacje:

1. Odczytać dane wejściowe użytkownika.
2. Zweryfikować poprawność daty, godziny, miasta i identyfikatorów atrakcji.
3. Pobrać szczegóły wybranych atrakcji.
4. Pobrać prognozę pogody.
5. Pobrać informacje o transporcie pomiędzy punktami.
6. Pobrać propozycje gastronomiczne, jeżeli użytkownik wybrał taką opcję.
7. Wyznaczyć kolejność atrakcji.
8. Obliczyć godziny rozpoczęcia i zakończenia poszczególnych aktywności.
9. Uwzględnić czas przejazdów.
10. Uwzględnić przerwy na posiłki.
11. Obliczyć koszt całkowity.
12. Obliczyć całkowity czas.
13. Zwrócić gotowy harmonogram i ostrzeżenia.

Nie implementuj skomplikowanego algorytmu optymalizacyjnego bez potrzeby.

Na początek wystarczy heurystyka:
- uwzględnianie czasu przejazdu,
- preferowanie atrakcji blisko siebie,
- uwzględnianie pogody,
- uwzględnianie czasu otwarcia, jeśli będzie dostępny,
- zachowanie godzin rozpoczęcia i zakończenia.

Użytkownik powinien mieć możliwość ręcznej zmiany kolejności atrakcji.

Nie twórz fałszywych danych o pogodzie, odległościach i cenach.

Jeżeli dane są niedostępne, oznacz je jako niedostępne.

## 10. Obsługa błędów i timeoutów

Każdy klient HTTP powinien mieć timeout 5000 ms.

Zaprojektuj jednolity mechanizm obsługi błędów.

Zasady:

- Atrakcje niedostępne – brak możliwości wygenerowania planu, HTTP 503.
- Pogoda niedostępna – plan można wygenerować bez pogody, ale dodaj ostrzeżenie.
- Transport niedostępny – plan można wygenerować bez czasów i dystansów przejazdu, dodaj ostrzeżenie.
- Gastronomia niedostępna – plan można wygenerować bez propozycji gastronomicznych, dodaj ostrzeżenie.
- Nieprawidłowe żądanie użytkownika – HTTP 400.
- Nieznana atrakcja – HTTP 404, jeżeli potwierdzono, że jej nie ma.
- Nieoczekiwany błąd – kontrolowana odpowiedź HTTP 500.

Nie zamieniaj każdego błędu HTTP na pustą listę.

Rozróżniaj brak danych, niedostępność usługi i błąd odpowiedzi.

Użyj `@RestControllerAdvice` do centralnej obsługi wyjątków.

## 11. Interfejs użytkownika – wymagania wizualne

To bardzo ważna część projektu.

Nie twórz zwykłego, nudnego panelu administracyjnego.

Chcemy nowoczesną aplikację przypominającą profesjonalne narzędzia do planowania podróży.

Inspiracja wizualna:
- nowoczesne aplikacje travel planner,
- minimalistyczne dashboardy,
- czytelne osie czasu,
- eleganckie karty,
- wyraźna typografia,
- jasne tło,
- delikatne cienie,
- zaokrąglone elementy,
- dobrze dobrane kolory.

Interfejs musi być responsywny.

### Widok 1 – Dashboard

Po wejściu do aplikacji użytkownik powinien zobaczyć:

- nazwę aplikacji, np. TripPlanner,
- krótki opis,
- przycisk utworzenia planu,
- główny obszar planowania,
- sekcję z funkcjonalnościami.

### Widok 2 – Kreator planu

Użytkownik powinien móc wybrać:

- miasto,
- datę,
- godzinę rozpoczęcia,
- punkt początkowy,
- środek transportu,
- interesujące go atrakcje,
- czy chce uwzględnić gastronomię,
- opcjonalnie czas zakończenia planowania.

Lista atrakcji powinna być pobierana z backendu, a nie wpisana na stałe.

Powinna umożliwiać:
- wyszukiwanie,
- filtrowanie kategorii,
- zaznaczanie wielu atrakcji,
- wyświetlanie cen,
- wyświetlanie czasu zwiedzania,
- rozróżnienie atrakcji krytych i otwartych.

### Widok 3 – Gotowy planer

To najważniejszy ekran aplikacji.

Po wygenerowaniu planu użytkownik powinien zobaczyć:

Górną sekcję podsumowania:
- miasto i data,
- pogoda,
- temperatura,
- liczba atrakcji,
- całkowity czas,
- całkowity koszt.

Główną część ekranu:
- pionowa oś czasu,
- karty poszczególnych atrakcji,
- godziny rozpoczęcia i zakończenia,
- czas zwiedzania,
- informacje o przejazdach,
- odległości,
- restauracje i przerwy na jedzenie.

Każdy typ elementu powinien być wizualnie rozróżnialny:
- atrakcja – ikona miejsca lub biletu,
- transport – ikona samochodu lub autobusu,
- gastronomia – ikona restauracji,
- pogoda – ikona warunków atmosferycznych.

Dodaj możliwość:
- zmiany kolejności atrakcji,
- usunięcia atrakcji,
- ponownego wygenerowania planu,
- powrotu do kreatora.

### Widok 4 – Szczegóły atrakcji

Po kliknięciu karty użytkownik powinien zobaczyć:
- nazwę,
- opis,
- kategorię,
- lokalizację,
- cenę,
- czas zwiedzania,
- informację, czy atrakcja jest kryta,
- koordynaty, jeśli są dostępne.

### Mapa

Jeżeli jest to możliwe na podstawie danych z API, dodaj mapę pokazującą:
- punkt początkowy,
- wybrane atrakcje,
- kolejność odwiedzania,
- trasę przejazdu.

Nie wymyślaj tras, jeżeli serwis Transport nie udostępnia takich informacji.

Możesz wykorzystać Leaflet i OpenStreetMap, jeżeli nie wymaga to płatnego klucza API.

### Obsługa stanów interfejsu

Zaimplementuj:
- ładowanie danych,
- skeleton loading,
- komunikaty o błędach,
- komunikaty o braku wyników,
- oznaczanie niedostępnych usług,
- czytelne ostrzeżenia,
- obsługę małych ekranów.

## 12. Organizacja kodu React

Nie umieszczaj całej aplikacji w jednym pliku.

Podziel ją na komponenty.

Przykładowe komponenty:

- `Navbar`
- `Dashboard`
- `PlannerForm`
- `CitySelector`
- `DateSelector`
- `AttractionList`
- `AttractionCard`
- `WeatherCard`
- `TransportCard`
- `GastronomyCard`
- `Timeline`
- `TimelineItem`
- `PlanSummary`
- `MapView`
- `LoadingState`
- `ErrorMessage`

Utwórz osobny katalog `services`, w którym znajdą się funkcje komunikujące się z backendem.

Nie wywołuj zewnętrznych API bezpośrednio z komponentów React.

## 13. Testy

Nie twórz nadmiarowych testów, ponieważ nie są one ważne w tym zadaniu, skup się na implementacji funkcjonalności.

## 14. Tryb demonstracyjny

Ponieważ adresy API zostaną dostarczone później, aplikacja musi być możliwa do uruchomienia już teraz.

Przygotuj kontrolowany tryb demonstracyjny.

Zasady:
- Tryb demonstracyjny musi być wyraźnie włączany konfiguracją.
- Dane demonstracyjne powinny być oznaczone jako przykładowe.
- Nie mogą udawać prawdziwych danych z API.
- Docelowa logika integracji musi być przygotowana niezależnie od trybu demonstracyjnego.
- Po otrzymaniu adresów usług powinno być możliwe przełączenie aplikacji na rzeczywiste API bez przebudowy frontendu.

Nie włączaj automatycznie trybu demonstracyjnego, gdy rzeczywista usługa nie odpowiada. Niedostępność usługi powinna być widoczna jako błąd, a nie zastępowana fałszywymi danymi.

## 15. Kolejność realizacji prac

Wykonuj pracę etapami.

### Etap 1 – Struktura projektu

- Utwórz katalog backend.
- Utwórz katalog frontend.
- Przygotuj Maven i Spring Boot.
- Przygotuj React i Vite.
- Skonfiguruj komunikację frontend-backend.
- Sprawdź, czy obie aplikacje się uruchamiają.

### Etap 2 – Backend i architektura integracji

- Utwórz pakiety config, controller, service, integration, dto, mapper i exception.
- Skonfiguruj WebClient.
- Przygotuj osobnych klientów dla czterech usług.
- Przygotuj konfigurację URL przez zmienne środowiskowe.
- Przygotuj timeouty.
- Przygotuj obsługę błędów.

### Etap 3 – Integracja Atrakcji

- Zaimplementuj prawdziwe formaty DTO wynikające z przekazanego kontraktu.
- Przygotuj pobieranie listy atrakcji.
- Przygotuj pobieranie szczegółów.
- Przygotuj endpointy naszego planera.
- Wyświetl dane w React.

### Etap 4 – Pozostałe integracje

- Dodaj klienta Pogody.
- Dodaj klienta Transportu.
- Dodaj klienta Gastronomii.
- Przygotuj mapowanie odpowiedzi.
- Zadbaj o częściową dostępność planera.

Jeżeli brakuje kontraktu konkretnego API, przygotuj interfejs, miejsce na DTO, konfigurację i testy z mockami. Nie wymyślaj docelowego kontraktu.

### Etap 5 – Logika planowania

- Pobieranie danych z wielu usług.
- Układanie harmonogramu.
- Obliczanie czasu.
- Obliczanie kosztów.
- Optymalizacja kolejności atrakcji.
- Uwzględnianie pogody.
- Uwzględnianie transportu.
- Obsługa przerw gastronomicznych.

### Etap 6 – Frontend

- Stwórz pełny interfejs.
- Stwórz kreator planu.
- Stwórz listę atrakcji.
- Stwórz widok harmonogramu.
- Stwórz karty pogody, transportu i gastronomii.
- Dodaj mapę, jeśli dostępne są współrzędne.
- Dodaj responsywność.
- Dopilnuj estetyki i spójności.

### Etap 7 – Nie twórz zbędnych testów


## 16. Ważne zasady pracy

1. Najpierw przeanalizuj istniejące pliki projektu. Nie usuwaj istniejącego kodu bez potrzeby.
2. Jeżeli repozytorium jest puste, utwórz kompletną strukturę od podstaw.
3. Nie twórz własnej bazy danych.
4. Nie używaj przykładowych danych jako domyślnego źródła informacji.
5. Nie wpisuj adresów API innych zespołów na sztywno.
6. Nie wymyślaj nieuzgodnionych endpointów.
7. Nie umieszczaj logiki biznesowej w kontrolerach.
8. Nie umieszczaj wywołań API w komponentach React.
9. Nie twórz jednego ogromnego komponentu ani jednej klasy obsługującej wszystko.
10. Zadbaj o czytelne nazewnictwo i podział odpowiedzialności.
11. Nie pozostawiaj pustych metod udających zaimplementowane funkcjonalności.
12. Nie ukrywaj błędów integracji.
13. Nie deklaruj ukończenia funkcji, jeśli nie została rzeczywiście zaimplementowana i przetestowana.
14. Jeżeli brakuje informacji od innych zespołów, zaznacz to w dokumentacji i przygotuj kod tak, aby można było łatwo uzupełnić integrację.
15. Po każdym etapie sprawdź, czy projekt się buduje i uruchamia.

## 17. Kryteria ukończenia

Projekt uznamy za gotowy do dalszej integracji, jeżeli:

- istnieją osobne katalogi backend i frontend,
- backend wykorzystuje Java 25, Spring Boot 4.1 i Maven,
- frontend wykorzystuje React i Vite,
- oba moduły można uruchomić niezależnie,
- frontend komunikuje się z naszym backendem,
- backend posiada własne REST API,
- przygotowano klientów integracyjnych dla czterech zespołów,
- adresy usług są konfigurowalne,
- przygotowano DTO i mapowanie danych,
- zaimplementowano obsługę timeoutów i błędów,
- istnieje logika tworzenia harmonogramu,
- interfejs umożliwia wybieranie atrakcji i generowanie planu,
- plan jest prezentowany na czytelnej osi czasu,
- aplikacja nie wymaga własnej bazy danych,
- istnieją drobne testy,
- istnieje dokumentacja uruchamiania.

## 18. Sposób zakończenia pracy

Po wykonaniu zadania przedstaw:

1. Strukturę utworzonego projektu.
2. Listę zaimplementowanych funkcjonalności.
3. Listę endpointów naszego backendu.
4. Listę zmiennych środowiskowych wymagających uzupełnienia.
5. Informację, które integracje są już rzeczywiście gotowe, a które oczekują na kontrakty API.
6. Instrukcję uruchomienia backendu i frontendu.
7. Wyniki testów i kompilacji.
8. Listę rzeczy, których nie udało się zrealizować.

Najważniejsze: stwórz działającą, estetyczną i dobrze uporządkowaną aplikację, która będzie solidną podstawą do późniejszej integracji z usługami pozostałych zespołów.

Nie kończ na samym opisie rozwiązania. Wykonuj zmiany bezpośrednio w plikach projektu.

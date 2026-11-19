# Frontend TripPlanner

React, Vite, Axios i Lucide React. Node.js 22.12+ lub 20.19+ z gałęzi 20.

## Uruchomienie

Z katalogu `frontend`:

```bash
npm install
npm run dev -- --host 0.0.0.0
```

Na komputerze planera otwórz `http://localhost:5173`, a z innego komputera w sieci otwórz `http://172.20.147.14:5173`, jeśli planer działa na hoście wskazanym w rejestrze. Domyślnie frontend wywołuje API pod tym samym hostem na porcie `8080`. Gdy frontend i backend działają na różnych komputerach lub backend używa innego portu, ustaw `VITE_API_BASE_URL` przez `.env` według `.env.example`. Dopuść również adres frontendu w `FRONTEND_ORIGIN` po stronie backendu. Oznaczenie trybu demo pochodzi z backendu.

```bash
npm run build
npm run preview
```

Podgląd kompilacji Vite korzysta domyślnie z portu 4173. Uruchom wtedy backend z `FRONTEND_ORIGIN=http://localhost:4173`.

## Obsługa

W trybie integracji możesz ustawić punkt startowy przyciskiem „Użyj punktu startowego w Warszawie” albo podać własne współrzędne w obszarze obsługiwanym przez Atrakcje. Wybierz datę, sposób dojazdu, liczbę osób i atrakcje z katalogu. Podczas generowania backend sprawdza dostępność pozycji w propozycjach dla wybranych parametrów. W widoku harmonogramu możesz zmienić kolejność lub usunąć atrakcję. Mapa pokazuje punkty, a przebieg trasy pojawia się, gdy odpowiedź planera zawiera geometrię przejazdu.

## Ilustracja ekranu startowego

Plik `public/warsaw-old-town.webp` powstał przy użyciu wbudowanego ImageGen. Prompt wykorzystany do wygenerowania ilustracji:

> Photorealistic editorial travel photograph for a Polish day-trip planner dashboard, wide landscape crop. Warsaw Old Town market square from a slightly elevated angle, colorful restored historic pastel townhouses with warm red tiled rooftops, soft late-afternoon clear light, a few distant pedestrians, rich but natural colors. Architecture concentrated center and right. No text, no logos, no graphic overlay. Premium city guide photography.

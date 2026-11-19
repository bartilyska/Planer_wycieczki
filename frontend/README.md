# Frontend TripPlanner

React, Vite, Axios i Lucide React. Node.js 22.12+ lub 20.19+ z gałęzi 20.

## Uruchomienie

Z katalogu `frontend`:

```bash
npm install
npm run dev -- --host 0.0.0.0
```

Na komputerze planera otwórz `http://localhost:5173`, a z innego komputera w sieci otwórz `http://172.20.147.15:5173`. Domyślnie frontend wywołuje API pod tym samym hostem na porcie `8080`. Gdy frontend i backend działają na różnych komputerach, ustaw `VITE_API_BASE_URL` przez `.env` według `.env.example`. Dopuść również adres frontendu w `FRONTEND_ORIGIN` po stronie backendu. Oznaczenie trybu demo pochodzi z backendu.

```bash
npm run build
npm run preview
```

Podgląd kompilacji Vite korzysta domyślnie z portu 4173. Uruchom wtedy backend z `FRONTEND_ORIGIN=http://localhost:4173`.

## Obsługa

Wybierz miasto z podpowiedzi katalogu lub wpisz je ręcznie. Po zaznaczeniu atrakcji możesz ustawić punkt startowy z jej szczegółów albo podać współrzędne. W gotowym harmonogramie przyciski kart pozwalają zmienić kolejność i usuwać atrakcje, a „Optymalizuj kolejność” wywołuje osobny endpoint. Mapa jest schematem punktów opartym na współrzędnych. Linia trasy jest wyświetlana, gdy odpowiedź planera zawiera geometrię przejazdu.

## Ilustracja ekranu startowego

Plik `public/warsaw-old-town.webp` powstał przy użyciu wbudowanego ImageGen. Prompt wykorzystany do wygenerowania ilustracji:

> Photorealistic editorial travel photograph for a Polish day-trip planner dashboard, wide landscape crop. Warsaw Old Town market square from a slightly elevated angle, colorful restored historic pastel townhouses with warm red tiled rooftops, soft late-afternoon clear light, a few distant pedestrians, rich but natural colors. Architecture concentrated center and right. No text, no logos, no graphic overlay. Premium city guide photography.

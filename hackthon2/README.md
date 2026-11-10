# Movie Review & Rating System

A dependency-free Java web application based on the provided problem statement. It includes a browser interface and a small HTTP API.

## Run

Requires JDK 17 or newer. From this folder, run in PowerShell:

```powershell
java --add-modules jdk.httpserver app.java
```

Open [http://localhost:8080](http://localhost:8080). The app binds to `127.0.0.1` and saves data in `data/movie-review.properties`.

To use a different port or data file:

```powershell
$env:PORT = "8090"
$env:MOVIE_DB_PATH = "data/my-movie-data.properties"
java --add-modules jdk.httpserver app.java
```

If startup says a port is already in use, the app may already be running; open its `http://localhost:<port>` address in your browser. To run another copy, set `PORT` to a free port such as `8081` before starting it.

## Features

- Add, list, edit, and delete movies.
- Register, list, edit, and delete viewers.
- Create, edit, and delete 1–5 star reviews.
- Submitting another review for the same viewer and movie updates the existing review and rating instead of creating a duplicate.
- Report and visually flag duplicate review pairs found in existing stored data; submitting for a pair with old duplicate records consolidates them into one updated review.
- Display each movie's average rating and review count, and filter by genre.
- Split the dashboard into separate Movies, Viewers, and Reviews tabs.
- Persist data across restarts in a local properties file.

Deleting a movie or viewer also deletes their associated reviews. Viewer email addresses must be unique.

## API

The UI uses these endpoints. Write requests use `application/x-www-form-urlencoded`.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/movies?genre=Drama` | List movies, optionally filtered by genre |
| `POST` | `/api/movies` | Add a movie |
| `PUT` / `DELETE` | `/api/movies/{id}` | Edit or delete a movie |
| `GET` | `/api/viewers` | List viewers |
| `POST` | `/api/viewers` | Register a viewer |
| `PUT` / `DELETE` | `/api/viewers/{id}` | Edit or delete a viewer |
| `GET` | `/api/reviews` | List reviews |
| `POST` | `/api/reviews` | Add a review |
| `PUT` / `DELETE` | `/api/reviews/{id}` | Edit or delete a review |
| `GET` | `/api/reviews/duplicates` | Find stored duplicate viewer/movie review pairs |

This starter runs locally without authentication and is not configured for public deployment.

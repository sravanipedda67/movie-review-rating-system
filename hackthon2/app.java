import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class App {
    private static final String PAGE = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Reel Reviews</title>
              <style>
                :root{font-family:Inter,Segoe UI,Arial,sans-serif;color:#172033;background:#f4f6fb}
                *{box-sizing:border-box}body{margin:0}header{background:#121a2b;color:#fff;padding:26px max(24px,calc((100% - 1120px)/2))}
                h1{margin:0;font-size:28px}header p{margin:7px 0 0;color:#b9c3d6}.wrap{max-width:1120px;margin:28px auto;padding:0 20px}
                .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:18px}.card{background:#fff;border:1px solid #e4e8f0;border-radius:14px;padding:20px;box-shadow:0 5px 18px #1720330a}
                h2{font-size:18px;margin:0 0 15px}.field{display:grid;gap:6px;margin:11px 0}label{font-size:13px;font-weight:650;color:#43506a}
                input,select,textarea{width:100%;border:1px solid #ccd3df;border-radius:8px;padding:10px 11px;font:inherit;color:#172033;background:white}
                textarea{min-height:84px;resize:vertical}button{border:0;border-radius:8px;padding:9px 13px;font:inherit;font-weight:650;cursor:pointer;background:#4359da;color:white}
                button.secondary{background:#edf0fb;color:#26377d}button.danger{background:#fff0ef;color:#a52a21}.actions{display:flex;gap:8px;margin-top:12px}
                .dashboard-nav{display:flex;gap:10px;flex-wrap:wrap;margin:20px 0 16px}.dashboard-nav button{background:#e5e9f4;color:#34405b}
                .dashboard-nav button.active{background:#4359da;color:white}.tab-panel[hidden]{display:none}
                .item{border-top:1px solid #edf0f5;padding:13px 0}.item:first-child{border-top:0}.title{font-weight:750}.muted{color:#69758a;font-size:13px}
                .stars{color:#d18b00;font-weight:700}.notice{padding:11px 13px;background:#fff7df;color:#6b4c00;border-radius:9px;margin:16px 0}
                .success{background:#e8f8ef;color:#17663a}.error{background:#fff0ef;color:#922c25}.toolbar{display:flex;gap:12px;align-items:end;flex-wrap:wrap}
                .toolbar .field{min-width:190px;flex:1}.empty{color:#7a8497;font-size:14px}.wide{grid-column:1/-1}
                @media(max-width:500px){header{padding:22px 18px}.wrap{padding:0 12px}.card{padding:16px}}
              </style>
            </head>
            <body>
            <header><h1>Reel Reviews</h1><p>A community space for movies, viewers, ratings and honest reviews.</p></header>
            <main class="wrap">
              <div id="message" class="notice" hidden></div>
              <div id="duplicateNotice"></div>
              <nav class="dashboard-nav" aria-label="Dashboard categories">
                <button type="button" class="active" data-tab="movies" aria-selected="true">Movies</button>
                <button type="button" data-tab="viewers" aria-selected="false">Viewers</button>
                <button type="button" data-tab="reviews" aria-selected="false">Reviews</button>
              </nav>
              <section id="moviesPanel" class="tab-panel">
                <div class="grid">
                  <article class="card">
                    <h2 id="movieHeading">Add a movie</h2>
                    <form id="movieForm">
                      <input type="hidden" name="id">
                      <div class="field"><label for="movieTitle">Title</label><input id="movieTitle" name="title" required maxlength="120"></div>
                      <div class="field"><label for="movieGenre">Genre</label><input id="movieGenre" name="genre" required maxlength="60" placeholder="Drama"></div>
                      <div class="field"><label for="movieYear">Release year</label><input id="movieYear" name="releaseYear" type="number" min="1888" max="2200" required></div>
                      <div class="field"><label for="movieDescription">Description</label><textarea id="movieDescription" name="description" maxlength="2000"></textarea></div>
                      <div class="actions"><button type="submit">Save movie</button><button class="secondary" type="button" onclick="resetMovie()">Cancel</button></div>
                    </form>
                  </article>
                  <article class="card">
                    <div class="toolbar"><div><h2 style="margin-bottom:4px">Movies</h2><span class="muted">Average ratings update automatically.</span></div>
                      <div class="field"><label for="genreFilter">Filter by genre</label><select id="genreFilter"><option value="">All genres</option></select></div>
                    </div>
                    <div id="movies"></div>
                  </article>
                </div>
              </section>
              <section id="viewersPanel" class="tab-panel" hidden>
                <div class="grid">
                  <article class="card">
                    <h2 id="viewerHeading">Register a viewer</h2>
                    <form id="viewerForm">
                      <input type="hidden" name="id">
                      <div class="field"><label for="viewerName">Name</label><input id="viewerName" name="name" required maxlength="100"></div>
                      <div class="field"><label for="viewerEmail">Email</label><input id="viewerEmail" name="email" type="email" required maxlength="200"></div>
                      <div class="actions"><button type="submit">Save viewer</button><button class="secondary" type="button" onclick="resetViewer()">Cancel</button></div>
                    </form>
                  </article>
                  <article class="card"><h2>Viewers</h2><div id="viewers"></div></article>
                </div>
              </section>
              <section id="reviewsPanel" class="tab-panel" hidden>
                <div class="grid">
                  <article class="card">
                    <h2 id="reviewHeading">Write a review</h2>
                    <form id="reviewForm">
                      <input type="hidden" name="id">
                      <div class="field"><label for="reviewViewer">Viewer</label><select id="reviewViewer" name="viewerId" required></select></div>
                      <div class="field"><label for="reviewMovie">Movie</label><select id="reviewMovie" name="movieId" required></select></div>
                      <div class="field"><label for="reviewRating">Rating (1–5 stars)</label><select id="reviewRating" name="rating" required><option value="5">5 stars</option><option value="4">4 stars</option><option value="3">3 stars</option><option value="2">2 stars</option><option value="1">1 star</option></select></div>
                      <div class="field"><label for="reviewText">Review</label><textarea id="reviewText" name="text" required maxlength="3000"></textarea></div>
                      <div class="actions"><button type="submit">Save review</button><button class="secondary" type="button" onclick="resetReview()">Cancel</button></div>
                    </form>
                  </article>
                  <article class="card"><h2>Reviews</h2><div id="reviews"></div></article>
                </div>
              </section>
            </main>
            <script>
            const $=s=>document.querySelector(s), enc=v=>encodeURIComponent(v);
            let movies=[], viewers=[], reviews=[];
            function switchTab(name){
              document.querySelectorAll(".tab-panel").forEach(panel=>panel.hidden=panel.id!==name+"Panel");
              document.querySelectorAll(".dashboard-nav button").forEach(button=>{
                const active=button.dataset.tab===name;
                button.classList.toggle("active",active);
                button.setAttribute("aria-selected",String(active));
              });
            }
            document.querySelectorAll(".dashboard-nav button").forEach(button=>button.addEventListener("click",()=>switchTab(button.dataset.tab)));
            function esc(v){return String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
            function msg(text,kind="success"){const e=$("#message");e.textContent=text;e.className="notice "+kind;e.hidden=false;setTimeout(()=>e.hidden=true,4500);}
            async function api(url,options={}){const r=await fetch(url,options);const body=await r.json();if(!r.ok)throw Error(body.error||"Request failed");return body;}
            function formBody(form){return new URLSearchParams(new FormData(form));}
            async function refresh(){
              try{
                const genre=$("#genreFilter").value;
                [movies,viewers,reviews]=await Promise.all([api("/api/movies"+(genre?"?genre="+enc(genre):"")),api("/api/viewers"),api("/api/reviews")]);
                renderMovies();renderViewers();renderReviews();renderSelects();await showDuplicates();
              }catch(e){msg(e.message,"error");}
            }
            function renderMovies(){
              const genres=[...new Set(movies.map(m=>m.genre).filter(Boolean))].sort();
              const filter=$("#genreFilter"), current=filter.value;
              filter.innerHTML='<option value="">All genres</option>'+genres.map(g=>`<option value="${esc(g)}">${esc(g)}</option>`).join("");
              filter.value=genres.includes(current)?current:"";
              $("#movies").innerHTML=movies.length?movies.map(m=>`<div class="item"><div class="title">${esc(m.title)} <span class="stars">${m.reviewCount?`★ ${m.averageRating.toFixed(1)}`:"☆ No ratings"}</span></div><div class="muted">${esc(m.genre)} · ${m.releaseYear} · ${m.reviewCount} review(s)</div><p>${esc(m.description||"")}</p><div class="actions"><button class="secondary" onclick="editMovie(${m.id})">Edit</button><button class="danger" onclick="deleteMovie(${m.id})">Delete</button></div></div>`).join(""):'<p class="empty">No movies yet. Add one above.</p>';
            }
            function renderViewers(){ $("#viewers").innerHTML=viewers.length?viewers.map(v=>`<div class="item"><div class="title">${esc(v.name)}</div><div class="muted">${esc(v.email)}</div><div class="actions"><button class="secondary" onclick="editViewer(${v.id})">Edit</button><button class="danger" onclick="deleteViewer(${v.id})">Delete</button></div></div>`).join(""):'<p class="empty">No viewers registered yet.</p>'; }
            function renderReviews(){
              const duplicateIds=new Set(reviews.filter(r=>r.duplicate).map(r=>r.id));
              $("#reviews").innerHTML=reviews.length?reviews.map(r=>`<div class="item"><div class="title">${esc(r.movieTitle)} <span class="stars">${"★".repeat(r.rating)}${"☆".repeat(5-r.rating)}</span></div><div class="muted">By ${esc(r.viewerName)} · ${new Date(r.updatedAt).toLocaleString()}</div>${duplicateIds.has(r.id)?'<div class="notice error">Duplicate record for this viewer and movie</div>':""}<p>${esc(r.text)}</p><div class="actions"><button class="secondary" onclick="editReview(${r.id})">Edit</button><button class="danger" onclick="deleteReview(${r.id})">Delete</button></div></div>`).join(""):'<p class="empty">No reviews yet.</p>';
            }
            function renderSelects(){
              const v=$("#reviewViewer"),m=$("#reviewMovie"),oldV=v.value,oldM=m.value;
              v.innerHTML='<option value="">Choose viewer</option>'+viewers.map(x=>`<option value="${x.id}">${esc(x.name)} (${esc(x.email)})</option>`).join("");
              m.innerHTML='<option value="">Choose movie</option>'+movies.map(x=>`<option value="${x.id}">${esc(x.title)}</option>`).join("");
              if(viewers.some(x=>x.id===Number(oldV)))v.value=oldV;if(movies.some(x=>x.id===Number(oldM)))m.value=oldM;
            }
            async function showDuplicates(){const groups=await api("/api/reviews/duplicates");$("#duplicateNotice").innerHTML=groups.length?`<div class="notice error"><strong>Existing duplicate reviews detected:</strong> ${groups.map(g=>`${esc(g.viewerName)} — ${esc(g.movieTitle)} (${g.reviewIds.length} records)`).join("; ")}. Edit or delete records to resolve them.</div>`:"";}
            function resetMovie(){const f=$("#movieForm");f.reset();f.elements.id.value="";$("#movieHeading").textContent="Add a movie";}
            function resetViewer(){const f=$("#viewerForm");f.reset();f.elements.id.value="";$("#viewerHeading").textContent="Register a viewer";}
            function resetReview(){const f=$("#reviewForm");f.reset();f.elements.id.value="";$("#reviewHeading").textContent="Write a review";}
            $("#movieForm").addEventListener("submit",async e=>{e.preventDefault();const f=e.currentTarget,id=f.elements.id.value;try{await api(id?`/api/movies/${id}`:"/api/movies",{method:id?"PUT":"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(f)});resetMovie();msg("Movie saved.");await refresh();}catch(x){msg(x.message,"error");}});
            $("#viewerForm").addEventListener("submit",async e=>{e.preventDefault();const f=e.currentTarget,id=f.elements.id.value;try{await api(id?`/api/viewers/${id}`:"/api/viewers",{method:id?"PUT":"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(f)});resetViewer();msg("Viewer saved.");await refresh();}catch(x){msg(x.message,"error");}});
            $("#reviewForm").addEventListener("submit",async e=>{e.preventDefault();const f=e.currentTarget,id=f.elements.id.value;const updating=Boolean(id)||reviews.some(r=>r.viewerId===Number(f.elements.viewerId.value)&&r.movieId===Number(f.elements.movieId.value));try{await api(id?`/api/reviews/${id}`:"/api/reviews",{method:id?"PUT":"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(f)});resetReview();msg(updating?"Review and rating updated.":"Review saved.");await refresh();}catch(x){msg(x.message,"error");}});
            $("#genreFilter").addEventListener("change",refresh);
            window.editMovie=id=>{const m=movies.find(x=>x.id===id),f=$("#movieForm");if(!m)return;switchTab("movies");for(const k of ["title","genre","releaseYear","description"])f.elements[k].value=m[k]??"";f.elements.id.value=id;$("#movieHeading").textContent="Edit movie";window.scrollTo({top:0,behavior:"smooth"});};
            window.editViewer=id=>{const v=viewers.find(x=>x.id===id),f=$("#viewerForm");if(!v)return;switchTab("viewers");f.elements.name.value=v.name;f.elements.email.value=v.email;f.elements.id.value=id;$("#viewerHeading").textContent="Edit viewer";window.scrollTo({top:0,behavior:"smooth"});};
            window.editReview=id=>{const r=reviews.find(x=>x.id===id),f=$("#reviewForm");if(!r)return;switchTab("reviews");f.elements.viewerId.value=r.viewerId;f.elements.movieId.value=r.movieId;f.elements.rating.value=r.rating;f.elements.text.value=r.text;f.elements.id.value=id;$("#reviewHeading").textContent="Edit review";window.scrollTo({top:0,behavior:"smooth"});};
            async function remove(url,label){if(!confirm(`Delete this ${label}?`))return;try{await api(url,{method:"DELETE"});msg(`${label[0].toUpperCase()+label.slice(1)} deleted.`);if(label==="movie")resetMovie();if(label==="viewer")resetViewer();if(label==="review")resetReview();await refresh();}catch(e){msg(e.message,"error");}}
            window.deleteMovie=id=>remove(`/api/movies/${id}`,"movie");window.deleteViewer=id=>remove(`/api/viewers/${id}`,"viewer");window.deleteReview=id=>remove(`/api/reviews/${id}`,"review");
            refresh();
            </script>
            </body>
            </html>
            """;

    private final Map<Long, Movie> movies = new TreeMap<>();
    private final Map<Long, Viewer> viewers = new TreeMap<>();
    private final Map<Long, Review> reviews = new TreeMap<>();
    private final Path databasePath;
    private long nextMovieId = 1;
    private long nextViewerId = 1;
    private long nextReviewId = 1;

    private App(Path databasePath) throws IOException {
        this.databasePath = databasePath;
        loadDatabase();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path databasePath = Path.of(System.getenv().getOrDefault("MOVIE_DB_PATH", "data/movie-review.properties"));
        App app = new App(databasePath);
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        } catch (BindException e) {
            System.err.println("Port " + port + " is already in use.");
            System.err.println("If the app is already running, open http://localhost:" + port + " instead.");
            System.err.println("Otherwise, choose another port in PowerShell, for example:");
            System.err.println("  $env:PORT = \"8081\"");
            System.err.println("  java --add-modules jdk.httpserver app.java");
            return;
        }
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Movie Review & Rating System running at http://localhost:" + port);
        System.out.println("Database file: " + databasePath.toAbsolutePath());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (ApiException e) {
            sendJson(exchange, e.status, jsonError(e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace(System.err);
            sendJson(exchange, 500, jsonError("Could not read or save application data."));
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            sendJson(exchange, 500, jsonError("An unexpected server error occurred."));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") && method.equals("GET")) {
            byte[] body = PAGE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            return;
        }

        if (path.equals("/api/movies")) {
            if (method.equals("GET")) {
                String genre = queryParams(exchange).getOrDefault("genre", "").trim();
                sendJson(exchange, 200, moviesJson(genre));
                return;
            }
            if (method.equals("POST")) {
                Map<String, String> form = readForm(exchange);
                synchronized (this) {
                    Movie movie = new Movie(nextMovieId++, required(form, "title"), required(form, "genre"),
                            year(form.get("releaseYear")), value(form, "description"));
                    movies.put(movie.id, movie);
                    saveDatabase();
                    sendJson(exchange, 201, movieJson(movie));
                }
                return;
            }
        }

        if (path.equals("/api/viewers")) {
            if (method.equals("GET")) {
                synchronized (this) {
                    sendJson(exchange, 200, viewersJson());
                }
                return;
            }
            if (method.equals("POST")) {
                Map<String, String> form = readForm(exchange);
                synchronized (this) {
                    String email = required(form, "email");
                    ensureUniqueEmail(email, 0);
                    Viewer viewer = new Viewer(nextViewerId++, required(form, "name"), email);
                    viewers.put(viewer.id, viewer);
                    saveDatabase();
                    sendJson(exchange, 201, viewerJson(viewer));
                }
                return;
            }
        }

        if (path.equals("/api/reviews/duplicates") && method.equals("GET")) {
            synchronized (this) {
                sendJson(exchange, 200, duplicatesJson());
            }
            return;
        }
        if (path.equals("/api/reviews")) {
            if (method.equals("GET")) {
                synchronized (this) {
                    sendJson(exchange, 200, reviewsJson());
                }
                return;
            }
            if (method.equals("POST")) {
                Map<String, String> form = readForm(exchange);
                synchronized (this) {
                    Review submitted = reviewFromForm(form, 0);
                    ensureReferenceExists(submitted);
                    List<Review> existingReviews = findReviews(submitted.viewerId, submitted.movieId);
                    Review review;
                    int status;
                    if (existingReviews.isEmpty()) {
                        review = new Review(nextReviewId++, submitted.viewerId, submitted.movieId,
                                submitted.rating, submitted.text, submitted.updatedAt);
                        status = 201;
                    } else {
                        Review existing = existingReviews.get(0);
                        review = new Review(existing.id, submitted.viewerId, submitted.movieId,
                                submitted.rating, submitted.text, submitted.updatedAt);
                        reviews.put(review.id, review);
                        existingReviews.stream().skip(1).forEach(duplicate -> reviews.remove(duplicate.id));
                        status = 200;
                    }
                    reviews.put(review.id, review);
                    saveDatabase();
                    sendJson(exchange, status, reviewJson(review));
                }
                return;
            }
        }

        String[] parts = path.split("/");
        if (parts.length == 4 && parts[1].equals("api")) {
            long id = parseId(parts[3]);
            if (parts[2].equals("movies")) {
                routeMovie(exchange, method, id);
                return;
            }
            if (parts[2].equals("viewers")) {
                routeViewer(exchange, method, id);
                return;
            }
            if (parts[2].equals("reviews")) {
                routeReview(exchange, method, id);
                return;
            }
        }
        throw new ApiException(404, "Endpoint not found.");
    }

    private synchronized void routeMovie(HttpExchange exchange, String method, long id) throws IOException {
        Movie existing = movies.get(id);
        if (existing == null) {
            throw new ApiException(404, "Movie not found.");
        }
        if (method.equals("PUT")) {
            Map<String, String> form = readForm(exchange);
            Movie updated = new Movie(id, required(form, "title"), required(form, "genre"),
                    year(form.get("releaseYear")), value(form, "description"));
            movies.put(id, updated);
            saveDatabase();
            sendJson(exchange, 200, movieJson(updated));
            return;
        }
        if (method.equals("DELETE")) {
            movies.remove(id);
            reviews.values().removeIf(review -> review.movieId == id);
            saveDatabase();
            sendJson(exchange, 200, "{\"deleted\":true}");
            return;
        }
        throw new ApiException(405, "Method not allowed.");
    }

    private synchronized void routeViewer(HttpExchange exchange, String method, long id) throws IOException {
        Viewer existing = viewers.get(id);
        if (existing == null) {
            throw new ApiException(404, "Viewer not found.");
        }
        if (method.equals("PUT")) {
            Map<String, String> form = readForm(exchange);
            String email = required(form, "email");
            ensureUniqueEmail(email, id);
            Viewer updated = new Viewer(id, required(form, "name"), email);
            viewers.put(id, updated);
            saveDatabase();
            sendJson(exchange, 200, viewerJson(updated));
            return;
        }
        if (method.equals("DELETE")) {
            viewers.remove(id);
            reviews.values().removeIf(review -> review.viewerId == id);
            saveDatabase();
            sendJson(exchange, 200, "{\"deleted\":true}");
            return;
        }
        throw new ApiException(405, "Method not allowed.");
    }

    private synchronized void routeReview(HttpExchange exchange, String method, long id) throws IOException {
        Review existing = reviews.get(id);
        if (existing == null) {
            throw new ApiException(404, "Review not found.");
        }
        if (method.equals("PUT")) {
            Map<String, String> form = readForm(exchange);
            Review updated = reviewFromForm(form, id);
            ensureReferenceExists(updated);
            boolean duplicate = findReviews(updated.viewerId, updated.movieId).stream()
                    .anyMatch(review -> review.id != id);
            if (duplicate) {
                throw new ApiException(409, "Another review already exists for this viewer and movie.");
            }
            reviews.put(id, updated);
            saveDatabase();
            sendJson(exchange, 200, reviewJson(updated));
            return;
        }
        if (method.equals("DELETE")) {
            reviews.remove(id);
            saveDatabase();
            sendJson(exchange, 200, "{\"deleted\":true}");
            return;
        }
        throw new ApiException(405, "Method not allowed.");
    }

    private synchronized void loadDatabase() throws IOException {
        if (!Files.exists(databasePath)) {
            return;
        }
        Properties data = new Properties();
        try (var input = Files.newInputStream(databasePath)) {
            data.load(input);
        }
        for (String key : data.stringPropertyNames()) {
            if (!key.startsWith("movie.") || !key.endsWith(".title")) {
                continue;
            }
            long id = recordId(key, "movie.", ".title");
            movies.put(id, new Movie(id, data.getProperty("movie." + id + ".title"),
                    data.getProperty("movie." + id + ".genre"), Integer.parseInt(data.getProperty("movie." + id + ".year")),
                    data.getProperty("movie." + id + ".description", "")));
            nextMovieId = Math.max(nextMovieId, id + 1);
        }
        for (String key : data.stringPropertyNames()) {
            if (!key.startsWith("viewer.") || !key.endsWith(".name")) {
                continue;
            }
            long id = recordId(key, "viewer.", ".name");
            viewers.put(id, new Viewer(id, data.getProperty("viewer." + id + ".name"),
                    data.getProperty("viewer." + id + ".email")));
            nextViewerId = Math.max(nextViewerId, id + 1);
        }
        for (String key : data.stringPropertyNames()) {
            if (!key.startsWith("review.") || !key.endsWith(".viewerId")) {
                continue;
            }
            long id = recordId(key, "review.", ".viewerId");
            reviews.put(id, new Review(id, Long.parseLong(data.getProperty("review." + id + ".viewerId")),
                    Long.parseLong(data.getProperty("review." + id + ".movieId")),
                    Integer.parseInt(data.getProperty("review." + id + ".rating")),
                    data.getProperty("review." + id + ".text"),
                    data.getProperty("review." + id + ".updatedAt", Instant.now().toString())));
            nextReviewId = Math.max(nextReviewId, id + 1);
        }
    }

    private synchronized void saveDatabase() throws IOException {
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties data = new Properties();
        for (Movie movie : movies.values()) {
            String prefix = "movie." + movie.id + ".";
            data.setProperty(prefix + "title", movie.title);
            data.setProperty(prefix + "genre", movie.genre);
            data.setProperty(prefix + "year", Integer.toString(movie.releaseYear));
            data.setProperty(prefix + "description", movie.description);
        }
        for (Viewer viewer : viewers.values()) {
            String prefix = "viewer." + viewer.id + ".";
            data.setProperty(prefix + "name", viewer.name);
            data.setProperty(prefix + "email", viewer.email);
        }
        for (Review review : reviews.values()) {
            String prefix = "review." + review.id + ".";
            data.setProperty(prefix + "viewerId", Long.toString(review.viewerId));
            data.setProperty(prefix + "movieId", Long.toString(review.movieId));
            data.setProperty(prefix + "rating", Integer.toString(review.rating));
            data.setProperty(prefix + "text", review.text);
            data.setProperty(prefix + "updatedAt", review.updatedAt);
        }
        Path temporary = databasePath.resolveSibling(databasePath.getFileName() + ".tmp");
        try (var output = Files.newOutputStream(temporary)) {
            data.store(output, "Movie Review & Rating System data");
        }
        try {
            Files.move(temporary, databasePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, databasePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String moviesJson(String genre) {
        synchronized (this) {
            List<Movie> filtered = movies.values().stream()
                    .filter(movie -> genre.isBlank() || movie.genre.equalsIgnoreCase(genre))
                    .toList();
            return "[" + filtered.stream().map(this::movieJson).collect(Collectors.joining(",")) + "]";
        }
    }

    private String movieJson(Movie movie) {
        List<Review> movieReviews = reviews.values().stream().filter(review -> review.movieId == movie.id).toList();
        double average = movieReviews.stream().mapToInt(review -> review.rating).average().orElse(0);
        return "{\"id\":" + movie.id + ",\"title\":" + quote(movie.title) + ",\"genre\":" + quote(movie.genre)
                + ",\"releaseYear\":" + movie.releaseYear + ",\"description\":" + quote(movie.description)
                + ",\"reviewCount\":" + movieReviews.size() + ",\"averageRating\":"
                + String.format(java.util.Locale.ROOT, "%.2f", average) + "}";
    }

    private String viewersJson() {
        return "[" + viewers.values().stream().map(this::viewerJson).collect(Collectors.joining(",")) + "]";
    }

    private String viewerJson(Viewer viewer) {
        return "{\"id\":" + viewer.id + ",\"name\":" + quote(viewer.name) + ",\"email\":" + quote(viewer.email) + "}";
    }

    private String reviewsJson() {
        return "[" + reviews.values().stream().map(this::reviewJson).collect(Collectors.joining(",")) + "]";
    }

    private String reviewJson(Review review) {
        Viewer viewer = viewers.get(review.viewerId);
        Movie movie = movies.get(review.movieId);
        boolean duplicate = findReviews(review.viewerId, review.movieId).size() > 1;
        return "{\"id\":" + review.id + ",\"viewerId\":" + review.viewerId + ",\"movieId\":" + review.movieId
                + ",\"viewerName\":" + quote(viewer == null ? "Unknown viewer" : viewer.name)
                + ",\"movieTitle\":" + quote(movie == null ? "Unknown movie" : movie.title)
                + ",\"rating\":" + review.rating + ",\"text\":" + quote(review.text)
                + ",\"updatedAt\":" + quote(review.updatedAt) + ",\"duplicate\":" + duplicate + "}";
    }

    private String duplicatesJson() {
        Map<String, List<Review>> groups = reviews.values().stream().collect(Collectors.groupingBy(
                review -> review.viewerId + ":" + review.movieId, TreeMap::new, Collectors.toList()));
        List<String> duplicates = new ArrayList<>();
        for (List<Review> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            Review first = group.get(0);
            Viewer viewer = viewers.get(first.viewerId);
            Movie movie = movies.get(first.movieId);
            String ids = group.stream().map(review -> Long.toString(review.id)).collect(Collectors.joining(","));
            duplicates.add("{\"viewerId\":" + first.viewerId + ",\"movieId\":" + first.movieId
                    + ",\"viewerName\":" + quote(viewer == null ? "Unknown viewer" : viewer.name)
                    + ",\"movieTitle\":" + quote(movie == null ? "Unknown movie" : movie.title)
                    + ",\"reviewIds\":[" + ids + "]}");
        }
        return "[" + String.join(",", duplicates) + "]";
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(65_537);
        if (bytes.length > 65_536) {
            throw new ApiException(413, "Request body is too large.");
        }
        return parseParams(new String(bytes, StandardCharsets.UTF_8));
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        return parseParams(query == null ? "" : query);
    }

    private static Map<String, String> parseParams(String input) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : input.split("&")) {
            if (pair.isEmpty()) continue;
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static Review reviewFromForm(Map<String, String> form, long id) {
        long viewerId = parseId(form.get("viewerId"));
        long movieId = parseId(form.get("movieId"));
        int rating;
        try {
            rating = Integer.parseInt(form.getOrDefault("rating", ""));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Rating must be a whole number from 1 to 5.");
        }
        if (rating < 1 || rating > 5) {
            throw new ApiException(400, "Rating must be from 1 to 5 stars.");
        }
        return new Review(id, viewerId, movieId, rating, required(form, "text"), Instant.now().toString());
    }

    private synchronized void ensureReferenceExists(Review review) {
        if (!viewers.containsKey(review.viewerId)) {
            throw new ApiException(400, "Select a registered viewer.");
        }
        if (!movies.containsKey(review.movieId)) {
            throw new ApiException(400, "Select an existing movie.");
        }
    }

    private synchronized List<Review> findReviews(long viewerId, long movieId) {
        return reviews.values().stream()
                .filter(review -> review.viewerId == viewerId && review.movieId == movieId)
                .toList();
    }

    private synchronized void ensureUniqueEmail(String email, long excludeId) {
        boolean used = viewers.values().stream().anyMatch(viewer -> viewer.id != excludeId
                && viewer.email.equalsIgnoreCase(email));
        if (used) {
            throw new ApiException(409, "That email address is already registered.");
        }
    }

    private static int year(String raw) {
        try {
            int year = Integer.parseInt(raw == null ? "" : raw);
            if (year < 1888 || year > 2200) {
                throw new ApiException(400, "Release year must be between 1888 and 2200.");
            }
            return year;
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Enter a valid release year.");
        }
    }

    private static long parseId(String raw) {
        try {
            long id = Long.parseLong(raw == null ? "" : raw);
            if (id < 1) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid record ID.");
        }
    }

    private static long recordId(String key, String prefix, String suffix) {
        return parseId(key.substring(prefix.length(), key.length() - suffix.length()));
    }

    private static String required(Map<String, String> form, String name) {
        String result = form.getOrDefault(name, "").trim();
        if (result.isEmpty()) {
            throw new ApiException(400, "The " + name + " field is required.");
        }
        return result;
    }

    private static String value(Map<String, String> form, String name) {
        return form.getOrDefault(name, "").trim();
    }

    private static String quote(String text) {
        StringBuilder json = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        return json.append('"').toString();
    }

    private static String jsonError(String message) {
        return "{\"error\":" + quote(message) + "}";
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private record Movie(long id, String title, String genre, int releaseYear, String description) {}
    private record Viewer(long id, String name, String email) {}
    private record Review(long id, long viewerId, long movieId, int rating, String text, String updatedAt) {}

    private static final class ApiException extends RuntimeException {
        private final int status;

        private ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}

#  Movie Review & Rating System

##  Project Overview

**Movie Reviews** is a web-based Movie Review and Rating System developed using Java. It provides a platform where users can explore movies, register as viewers, submit movie reviews, and rate movies based on their experience.

The application helps users share their opinions about movies and view average ratings based on community reviews.

##  Objectives

- To develop a simple and user-friendly movie review platform.
- To allow users to add, update, and delete movie details.
- To register and manage viewers.
- To enable viewers to submit movie reviews and ratings.
- To calculate average movie ratings automatically.
- To identify and manage duplicate reviews.
- To store application data persistently.

## Technologies Used

| Technology | Purpose |
|---|---|
| Java | Backend development |
| HTML5 | Web page structure |
| CSS3 | Styling and responsive design |
| JavaScript | Frontend functionality |
| Java HttpServer | HTTP server and API handling |
| Properties File | Data storage |
| REST API | Communication between frontend and backend |

##  Features

### 1. Movie Management
- Add new movies.
- Edit existing movie details.
- Delete movies.
- View movie information.
- Filter movies based on genre.
- Display average ratings automatically.

### 2. Viewer Management
- Register new viewers.
- Store viewer names and email addresses.
- Edit viewer details.
- Delete viewer records.
- Prevent duplicate email registrations.

### 3. Review & Rating Management
- Submit movie reviews.
- Give ratings from 1 to 5 stars.
- Edit existing reviews.
- Delete reviews.
- Display viewer names and movie details.
- Automatically calculate average ratings.
- Detect duplicate reviews for the same viewer and movie.

### 4. User Interface
- Simple and responsive dashboard.
- Separate Movies, Viewers, and Reviews sections.
- Easy navigation.
- Genre-based movie filtering.
- Success and error notifications.

##  Project Architecture

The project follows a simple client-server architecture.

**Frontend:**
- HTML
- CSS
- JavaScript

**Backend:**
- Java
- Built-in Java HTTP Server
- REST API endpoints

**Storage:**
- Java Properties File

### Application Workflow

1. User opens the Reel Reviews application.
2. Java HTTP Server loads the web interface.
3. User selects Movies, Viewers, or Reviews.
4. Frontend sends HTTP requests to Java REST API endpoints.
5. Backend processes the requested operations.
6. Application updates the data file.
7. Updated information is displayed on the dashboard.

##  Project Structure

```text
Reel-Reviews/
│
├── App.java
│
├── data/
│   └── movie-review.properties
│
└── README.md
```

**Note:** The database properties file is automatically created when application data is saved.

##  Installation and Execution

### Prerequisites

- Java JDK 17 or above.
- Visual Studio Code or any Java-supported IDE.
- Web browser.

### Step 1: Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### Step 2: Open the Project Folder

```bash
cd Reel-Reviews
```

### Step 3: Compile the Java Program

```bash
javac --add-modules jdk.httpserver App.java
```

### Step 4: Run the Application

```bash
java --add-modules jdk.httpserver App
```

### Step 5: Open in Browser

Open the following address:

```text
http://localhost:8080
```

The Reel Reviews application will open in your browser.

##  API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Load application homepage |
| GET | `/api/movies` | Get all movies |
| POST | `/api/movies` | Add a movie |
| PUT | `/api/movies/{id}` | Update movie |
| DELETE | `/api/movies/{id}` | Delete movie |
| GET | `/api/viewers` | Get all viewers |
| POST | `/api/viewers` | Register a viewer |
| PUT | `/api/viewers/{id}` | Update viewer |
| DELETE | `/api/viewers/{id}` | Delete viewer |
| GET | `/api/reviews` | Get all reviews |
| POST | `/api/reviews` | Submit a review |
| PUT | `/api/reviews/{id}` | Update review |
| DELETE | `/api/reviews/{id}` | Delete review |
| GET | `/api/reviews/duplicates` | Detect duplicate reviews |

##  Rating Calculation

The system calculates the average rating of each movie automatically.

**Formula:**

Average Rating = Total Ratings / Number of Reviews

Ratings are displayed using a five-star rating system.

##  Future Enhancements

- User authentication and login.
- Movie posters and trailers.
- Search movies by title.
- Advanced movie recommendations.
- Admin dashboard.
- Integration with MySQL database.
- Cloud deployment.
- Improved security and user access management.

##  Developed By

** Movie Review & Rating System**

Developed as a Java-based web application project.

##  License

This project is developed for educational and learning purposes.

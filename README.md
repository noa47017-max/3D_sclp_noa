# 3D Container Loading Optimizer

A full-stack web application that solves the **3D Single Container Loading Problem (3D-SCLP)**: given a container and a list of box types, it computes a packing that maximizes the container's **Volume Utilization Rate (VUR)** and renders the result as an animated 3D loading simulation.

The core is a **hybrid Ant Colony Optimization (ACO) algorithm** written from scratch in Java, running many ants in parallel on a Spring Boot backend. The frontend is built with vanilla JavaScript and Three.js.

> Final project in Software Engineering.

<!-- Add a screenshot or GIF of the 3D simulation here, for example:
![3D loading simulation](docs/demo.gif)
-->

---

## Features

- **3D visualization**: the packing is animated box by box in Three.js. You can replay it, orbit the camera, and export the animation as a `.webm` video.
- **Orientation constraints per box type**: each box type can allow 6, 2, or 1 orientation, for example "this side up".
- **Benchmark runner**: load a standard **BR (Bischoff & Ratcliff)** test file, run a single case or all cases, and see the VUR and runtime for each one.
- **History**: every packing result is saved in MongoDB and can be reopened and replayed later.
- **Export**: download the placement list as CSV, or print it.
- **User accounts**: registration and login.

## The Algorithm

The solver follows a layer-based construction heuristic guided by an ant colony:

```
for each iteration τ = 1..M (100):
    run m (30) ants in parallel:
        fill the container layer by layer:
            pick the next empty space (sorted by position)
            choose box + orientation   ← GSM or ASM (see below)
            place it, split the leftover into 3 residual spaces,
            and cut away overlapping spaces
            prune the ant early if its partial solution is clearly worse
        merge leftover spaces at the end and try to fill them
    evaporate pheromones globally, reinforce the iteration-best solution
return the global-best solution
```

**Two selection strategies, mixed over time** ([`StrategySelector`](src/main/java/com/noa/packer/algorithms/StrategySelector.java))

| Strategy | How it chooses | When |
|---|---|---|
| **GSM**, Greedy Selection Method | The box with the highest volume ratio to the current space, in its best orientation | Mostly in early iterations |
| **ASM**, Ant Selection Method | Roulette-wheel selection weighted by `τ^α · η^β`: pheromone × a heuristic that rewards exact fits and penalizes unusable leftover gaps | Mostly in later iterations |

The probability of choosing GSM decays with the iteration number: `p(τ) = exp(-(3τ/M)² / 2)`. This gives broad greedy exploration at the start and pheromone-driven exploitation at the end.

**Key components**

- [`HybridLoadingSystem`](src/main/java/com/noa/packer/algorithms/HybridLoadingSystem.java): the main ACO loop. It runs the ants in parallel with `CompletableFuture`, then applies evaporation and reinforcement.
- [`AntLoadingEngine`](src/main/java/com/noa/packer/algorithms/AntLoadingEngine.java): builds a single ant's solution, layer by layer.
- [`PheromoneManager`](src/main/java/com/noa/packer/algorithms/PheromoneManager.java): a thread-safe pheromone tree (`ConcurrentSkipListMap`) keyed by a "state word" that encodes the remaining cargo. It handles local and global updates, and its size is capped to bound memory.
- [`SpaceManager`](src/main/java/com/noa/packer/algorithms/SpaceManager.java): the geometry of empty spaces: cutting them around a placed box, merging adjacent or overlapping spaces, and removing spaces contained in other spaces.
- [`PruningManager`](src/main/java/com/noa/packer/algorithms/PruningManager.java): stops an ant early when its accumulated waste is much worse than the best seen for the same state.

## Tech Stack

| Layer | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3.2 (Web, Security, Data MongoDB, Validation) |
| Database | MongoDB Atlas |
| Security | JWT with sliding expiration, BCrypt password hashing, HTTPS, per-client rate limiting, input sanitization |
| Concurrency | Two separate thread pools: one for algorithm workers (ants) and one for incoming solve requests. Heavy solves cannot starve the server. |
| Frontend | Vanilla JavaScript (ES modules), Three.js, HTML/CSS (RTL Hebrew UI) |

## Project Structure

```
src/main/java/com/noa/packer/
├── algorithms/     # ACO solver: ants, strategies, pheromones, space management
├── controllers/    # REST API: /api/solve, /api/history, /api/users/*
├── services/       # Business logic: packing, users
├── models/ dto/    # MongoDB documents and request/response objects
├── repositories/   # Spring Data MongoDB repositories
├── security/       # JWT, user details, rate limiter
└── config/         # Spring Security, thread pools, global error handling
src/main/resources/static/   # Frontend: index.html, app.js, style.css
```

## API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/users/register` | Create an account |
| `POST` | `/api/users/login` | Log in and receive a JWT |
| `POST` | `/api/solve` | Run the solver on a container and a list of boxes |
| `GET`  | `/api/history` | List the current user's past packings |
| `GET`  | `/api/history/{id}` | Get a saved packing |

## Running Locally

**Prerequisites:** Java 17+, Maven, and a MongoDB database (a free MongoDB Atlas cluster or a local `mongod`).

1. **Clone the repository**
   ```bash
   git clone https://github.com/noa47017-max/3d-container-loading-aco.git
   cd 3d-container-loading-aco
   ```

2. **Create the configuration file**
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   Fill in your MongoDB URI and a random `jwt.secret`. For a local database you can use `mongodb://localhost:27017/packer`.

3. **Generate a self-signed HTTPS certificate** in the project root
   ```bash
   keytool -genkeypair -alias springboot -keyalg RSA -keysize 2048 \
           -storetype PKCS12 -keystore keystore.p12 -validity 3650
   ```
   Use the same password you set in `server.ssl.key-store-password`.

4. **Run**
   ```bash
   mvn spring-boot:run
   ```
   Then open <https://localhost:8443>. Your browser will warn about the self-signed certificate. This is expected locally.

## Author

**Noa**: [github.com/noa47017-max](https://github.com/noa47017-max)

# 📚 Book Review Service

Servizio REST API per la gestione di recensioni di libri, sviluppato con Spring Boot 3.4.0 e Java 21.

## 🎯 Obiettivo del Progetto

Questo progetto implementa un servizio RESTful completo che permette di:
- **Cercare libri** tramite l'API pubblica Gutendex
- **Creare recensioni** per i libri trovati
- **Gestire recensioni** (lettura, modifica, cancellazione)
- **Processamento asincrono** per arricchire le recensioni con metadati

## 🛠 Tecnologie Utilizzate

| Tecnologia | Versione | Scopo |
|------------|----------|-------|
| **Java** | 21 LTS | Linguaggio di programmazione |
| **Spring Boot** | 3.4.0 | Framework applicativo |
| **Spring Data JPA** | 3.4.0 | Accesso ai dati |
| **PostgreSQL** | Latest | Database produzione |
| **H2 Database** | 2.2.224 | Database sviluppo/test |
| **Maven** | 3.6+ | Build automation |
| **Docker** | Latest | Containerizzazione |
| **JUnit 5** | 5.10.1 | Testing |

## 🚀 Avvio Rapido

### Con Docker (Raccomandato)
```bash
docker-compose up --build
```

### Test
```bash
# Con Docker
docker build -f Dockerfile.test -t book-review-test .
docker run --rm book-review-test

# Locale (richiede Java 21)
mvn clean test
```

### Accesso
- **API**: http://localhost:8080
- **Console H2** (dev): http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/actuator/health

## 📡 API Endpoints

### 1. Cerca Libri
```http
GET /book/search?q=dickens
```
Cerca libri usando l'API Gutendex.

**Esempio risposta:**
```json
{
  "count": 156,
  "results": [
    {
      "id": 98,
      "title": "A Tale of Two Cities",
      "authors": [{"name": "Charles Dickens"}]
    }
  ]
}
```

### 2. Crea Recensione
```http
POST /review
Content-Type: application/json

{
  "id": "98",
  "review": "Un capolavoro intramontabile della letteratura inglese.",
  "score": 5
}
```

**Risposta:** `202 Accepted` - La recensione viene creata e messa in coda per il processamento asincrono.

### 3. Leggi Recensione
```http
GET /review/{id}
```

**Possibili risposte:**
- `200 OK` - Recensione completata e pronta
- `202 Accepted` - Recensione in elaborazione
- `404 Not Found` - Recensione non esistente

### 4. Aggiorna Recensione
```http
PUT /review/{id}
Content-Type: application/json

{
  "review": "Testo aggiornato della recensione",
  "score": 4
}
```

### 5. Elimina Recensione
```http
DELETE /review/{id}
```

**Risposta:** `204 No Content`

## 🏗 Architettura

Il progetto segue un'architettura a livelli (layered architecture):

```
┌─────────────────────────┐
│   Controller Layer      │  ← Gestione HTTP, validazione input
├─────────────────────────┤
│   Service Layer         │  ← Logica di business
├─────────────────────────┤
│   Repository Layer      │  ← Accesso ai dati (Spring Data JPA)
├─────────────────────────┤
│   Entity Layer          │  ← Modelli di dominio
└─────────────────────────┘
```

## ✨ Caratteristiche Principali

### 1. **Processamento Asincrono**
Le recensioni vengono arricchite in background con metadati dei libri (titolo, autore) senza bloccare la risposta HTTP.

### 2. **Validazione Robusta**
- Validazione Bean (JSR-380) su tutti i DTO
- Controllo esistenza libro tramite API Gutendex
- Gestione errori centralizzata con `@ControllerAdvice`

### 3. **Database Multipli**
- **H2** in-memory per sviluppo e testing (veloce, zero configurazione)
- **PostgreSQL** per produzione (affidabile, scalabile)

### 4. **Configurazione per Profili**
- `default`: PostgreSQL
- `dev`: H2 con console abilitata
- `test`: H2 in-memory per test
- `prod`: PostgreSQL con ottimizzazioni

### 5. **Connection Pooling**
HikariCP configurato per performance ottimali:
- Pool size: 10 connessioni (dev), 20 (prod)
- Timeout: 30 secondi
- Leak detection abilitato

### 6. **Testing Completo**
- **28 test** totali (27 passano sempre, 1 richiede API esterna)
- Test unitari per controller, service, client
- Test di integrazione end-to-end
- Coverage target: 80%+

## 🐳 Docker

### Build Multi-stage
```dockerfile
# Stage 1: Build con Maven e Java 21
FROM eclipse-temurin:21-jdk-alpine
# ... compila l'applicazione

# Stage 2: Runtime con JRE minimal
FROM eclipse-temurin:21-jre-alpine
# ... esegue solo il JAR
```

### Caratteristiche Security
- ✅ Utente non-root (`spring:spring`)
- ✅ JRE invece di JDK (immagine più piccola)
- ✅ Health check configurato
- ✅ JVM ottimizzato per container

## 📊 Qualità del Codice

### Plugin Maven
- **JaCoCo**: Code coverage (target 80%)
- **SpotBugs**: Analisi statica per bug comuni
- **Maven Compiler**: Java 21, release flag abilitato

### Best Practices
- Constructor-based dependency injection
- Null-safety con `@NonNull`
- Logging strutturato (SLF4J)
- Exception handling centralizzato
- DTOs separati dagli Entity

## 🗄 Configurazione Database

### Development (H2)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:
```

### Production (PostgreSQL)
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookreview
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
```

## 📈 Monitoraggio

Spring Boot Actuator endpoints disponibili:
- `/actuator/health` - Stato applicazione
- `/actuator/info` - Informazioni build
- `/actuator/metrics` - Metriche runtime

## 🔧 Configurazione Locale

### Prerequisiti
- **Java 21** (Eclipse Temurin raccomandato)
- **Maven 3.6+**
- **Docker** (opzionale ma raccomandato)

### Setup
```bash
# Clone repository
git clone <repository-url>
cd book-review-service

# Build
mvn clean package

# Run
java -jar target/book-review-service-1.0.0.jar

# Oppure con Maven
mvn spring-boot:run
```

## 📝 Note sul Design

### Perché Processamento Asincrono?
Le chiamate all'API Gutendex possono richiedere tempo. Processando in background:
- ✅ Risposta HTTP immediata (migliore UX)
- ✅ Non blocca thread del server
- ✅ Resilienza a errori API esterni

### Perché H2 per Development?
- ✅ Zero configurazione
- ✅ Veloce per test
- ✅ In-memory (pulizia automatica)
- ✅ Console web integrata

### Perché PostgreSQL per Production?
- ✅ ACID compliant
- ✅ Scalabile
- ✅ Feature avanzate (JSON, full-text search)
- ✅ Ampia adozione enterprise

## 🎓 Concetti Applicati

### Design Patterns
- **Repository Pattern**: Astrazione accesso dati
- **DTO Pattern**: Separazione API da domain model
- **Dependency Injection**: IoC con Spring
- **Factory Pattern**: RestTemplate via builder

### Principi SOLID
- **Single Responsibility**: Ogni classe ha un compito preciso
- **Open/Closed**: Estensibile via interfacce
- **Dependency Inversion**: Dipendenza da astrazioni

### Best Practices Spring
- Constructor injection (immutabilità)
- `@Transactional` per operazioni atomiche
- Exception translation layer
- Profile-based configuration

## 📚 Struttura Codice

```
src/main/java/com/squassi/bookreview/
├── BookReviewApplication.java      # Entry point
├── config/                         # Configurazioni Spring
│   ├── AsyncConfiguration.java     # Thread pool async
│   └── WebConfiguration.java       # Web/CORS config
├── controller/                     # REST controllers
│   ├── BookController.java
│   └── ReviewController.java
├── service/                        # Business logic
│   ├── ReviewService.java
│   ├── GutendexClient.java
│   └── AsyncProcessor.java
├── repository/                     # Data access
│   └── ReviewRepository.java
├── entity/                         # JPA entities
│   └── ReviewEntity.java
├── dto/                           # Data Transfer Objects
│   └── ReviewRequestDto.java
├── exception/                      # Custom exceptions
│   └── GlobalExceptionHandler.java
├── enums/                         # Enumerations
│   └── ReviewStatus.java
└── constants/                     # Application constants
    └── ApplicationConstants.java
```

## 🚨 Troubleshooting

### Port 8080 occupato
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Test falliscono
```bash
# Usa Docker (garantito Java 21)
docker build -f Dockerfile.test -t book-review-test .
docker run --rm book-review-test
```

### Docker build lento
```bash
# Pulisci cache
docker system prune -a
```

## 📄 Licenza

MIT License - Vedi LICENSE file per dettagli.

---

## 👨‍💻 Sviluppato da

Manuel Squassi

**Technical Assessment Project** - Dimostrazione competenze:
- Spring Boot 3.x moderno
- Design API RESTful
- Pattern asincroni
- Containerizzazione
- Testing completo
- Codice production-ready

---

**Java 21 + Spring Boot 3.4.0** ☕

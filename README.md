# Database Performance Monitor

A comprehensive Spring Boot application for monitoring database performance, similar to SolarWinds Database Performance Analyzer. This application provides real-time monitoring of database metrics, connection pool status, query performance, and system alerts.

## Features

### 🎯 Core Functionality

1. **Real-Time Dashboard**
   - Live database status monitoring
   - Connection pool metrics
   - CPU and memory usage tracking
   - Real-time charts and visualizations
   - Auto-refresh every 5 seconds

2. **Performance Monitoring**
   - Historical performance data
   - CPU usage trends
   - Memory usage trends
   - Connection pool utilization over time
   - Customizable time ranges

3. **Query Monitoring**
   - Track all database queries
   - Identify slow queries (configurable threshold)
   - Query execution time analysis
   - Query type categorization (SELECT, INSERT, UPDATE, DELETE)
   - Failed query tracking

4. **Connection Pool Management**
   - Active connections monitoring
   - Idle connections tracking
   - Pool utilization visualization
   - Waiting threads detection
   - HikariCP integration

5. **Alert System**
   - Automatic alert generation for:
     - High CPU usage
     - High memory usage
     - Connection pool saturation
     - Excessive waiting threads
   - Alert severity levels (CRITICAL, WARNING, INFO)
   - Alert acknowledgment and management
   - Alert history

6. **REST API**
   - `/api/metrics/current` - Get current metrics
   - `/api/metrics/history` - Get historical metrics
   - `/api/metrics/summary` - Get metrics summary
   - `/api/queries/*` - Query monitoring endpoints
   - `/api/alerts/*` - Alert management endpoints

## Technology Stack

- **Backend**: Spring Boot 3.2.0
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Charts**: Chart.js
- **Database**: H2 (in-memory, for demo), MySQL/PostgreSQL support
- **Connection Pool**: HikariCP
- **Monitoring**: Spring Boot Actuator, Micrometer
- **Build Tool**: Maven
- **Java Version**: 17

## Project Structure

```
database-performance-monitor/
├── src/
│   ├── main/
│   │   ├── java/com/dbmonitor/
│   │   │   ├── DatabaseMonitorApplication.java
│   │   │   ├── model/
│   │   │   │   ├── DatabaseMetrics.java
│   │   │   │   ├── QueryMetrics.java
│   │   │   │   └── Alert.java
│   │   │   ├── repository/
│   │   │   │   ├── QueryMetricsRepository.java
│   │   │   │   └── AlertRepository.java
│   │   │   ├── service/
│   │   │   │   ├── DatabaseMonitoringService.java
│   │   │   │   ├── QueryMonitoringService.java
│   │   │   │   └── AlertService.java
│   │   │   ├── controller/
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── MetricsApiController.java
│   │   │   │   ├── QueryApiController.java
│   │   │   │   └── AlertApiController.java
│   │   │   └── config/
│   │   │       └── DataInitializer.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/
│   │           ├── dashboard.html
│   │           ├── performance.html
│   │           ├── queries.html
│   │           ├── connections.html
│   │           └── alerts.html
│   └── test/
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Installation & Running

1. **Clone or extract the project**

2. **Navigate to project directory**
   ```bash
   cd db-monitor
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR file:
   ```bash
   java -jar target/database-performance-monitor-1.0.0.jar
   ```

5. **Access the application**
   - Main Dashboard: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - Actuator Endpoints: http://localhost:8080/actuator

### Default Configuration

The application uses H2 in-memory database by default for demonstration purposes.

**H2 Console Access:**
- URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## Configuration

### Database Configuration

To connect to MySQL:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

To connect to PostgreSQL:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Monitoring Configuration

Adjust thresholds in `application.properties`:

```properties
# Slow query threshold (milliseconds)
monitor.query.slow-threshold-ms=1000

# Alert thresholds
monitor.alert.cpu-threshold=80
monitor.alert.memory-threshold=85

# Refresh interval (milliseconds)
monitor.refresh.interval-ms=5000
```

### HikariCP Configuration

Customize connection pool settings:

```properties
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1200000
```

## Usage Guide

### Dashboard
- View real-time database metrics
- Monitor connection pool status
- Track CPU and memory usage
- See active alerts

### Performance
- Analyze historical performance data
- View trends over time
- Identify performance bottlenecks

### Queries
- View all executed queries
- Identify slow queries
- Analyze query patterns
- Track failed queries

### Connections
- Monitor connection pool utilization
- View active and idle connections
- Track waiting threads
- Analyze pool configuration

### Alerts
- View active alerts
- Acknowledge or dismiss alerts
- Filter by severity
- Review alert history

## API Endpoints

### Metrics API

```
GET /api/metrics/current
GET /api/metrics/history?limit=50
GET /api/metrics/summary
GET /api/metrics/chart-data?limit=30
```

### Query API

```
GET /api/queries
GET /api/queries/slow
GET /api/queries/slow/recent?hours=24
GET /api/queries/statistics
POST /api/queries/record
```

### Alert API

```
GET /api/alerts
GET /api/alerts/unacknowledged
GET /api/alerts/recent?hours=24
POST /api/alerts/{id}/acknowledge
POST /api/alerts/acknowledge-all
DELETE /api/alerts/{id}
```

## Monitoring Features

### Collected Metrics

1. **Connection Metrics**
   - Active connections
   - Idle connections
   - Total connections
   - Connection pool usage percentage
   - Waiting threads

2. **Performance Metrics**
   - CPU usage percentage
   - Memory usage percentage
   - Query execution times
   - Slow query count

3. **Database Info**
   - Database type and version
   - Connection status
   - Uptime

### Alert Conditions

Alerts are automatically generated when:
- CPU usage exceeds configured threshold (default: 80%)
- Memory usage exceeds configured threshold (default: 85%)
- Connection pool usage exceeds 90%
- More than 10 threads are waiting for connections

## Sample Data

The application includes a data initializer that generates sample queries for demonstration:
- Simulates various query types (SELECT, INSERT, UPDATE, DELETE)
- Creates both fast and slow queries
- Runs every 10 seconds

To disable sample data generation, remove or comment out the `DataInitializer` class.

## Customization

### Adding New Metrics

1. Add properties to `DatabaseMetrics` class
2. Update `DatabaseMonitoringService.getCurrentMetrics()`
3. Update templates to display new metrics

### Custom Alert Rules

1. Add new alert conditions in `DatabaseMonitoringService.checkAndCreateAlerts()`
2. Define new alert types and thresholds
3. Update alert templates

### Database Support

The application supports:
- H2 (default, in-memory)
- MySQL
- PostgreSQL
- Any JDBC-compliant database

Simply update the datasource configuration in `application.properties`.

## Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   - Change port in `application.properties`: `server.port=8081`

2. **Database connection failed**
   - Verify database is running
   - Check connection credentials
   - Ensure database driver is in classpath

3. **Charts not displaying**
   - Check browser console for JavaScript errors
   - Ensure Chart.js CDN is accessible
   - Clear browser cache

## Performance Considerations

- The application stores metrics history in memory (last 100 data points)
- For production use, consider persisting metrics to a time-series database
- Adjust refresh intervals based on your needs
- Configure connection pool sizes according to your workload

## Future Enhancements

Potential features to add:
- Email/SMS alert notifications
- Custom dashboard widgets
- Export reports (PDF, Excel)
- Multi-database monitoring
- User authentication and authorization
- Metric persistence to time-series database
- Query plan analysis
- Index recommendations
- Backup monitoring
- Replication lag tracking

## License

This project is provided as-is for educational and demonstration purposes.

## Support

For issues or questions, please refer to the code comments or Spring Boot documentation.

---

**Built with Spring Boot 3.2.0, Thymeleaf, and Chart.js**

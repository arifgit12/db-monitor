# Database Monitor - Enterprise API Documentation

## Table of Contents
1. [Authentication & Users](#authentication--users)
2. [Notifications](#notifications)
3. [Dashboard Widgets](#dashboard-widgets)
4. [Reports & Export](#reports--export)
5. [Backup Monitoring](#backup-monitoring)
6. [Replication Monitoring](#replication-monitoring)
7. [Query Plan Analysis](#query-plan-analysis)
8. [Index Recommendations](#index-recommendations)

---

## Authentication & Users

### User Management

#### Get All Users
```http
GET /api/users
```

#### Get User by ID
```http
GET /api/users/{id}
```

#### Get User by Username
```http
GET /api/users/username/{username}
```

#### Create User
```http
POST /api/users
Content-Type: application/json

{
  "username": "john.doe",
  "password": "securePassword123",
  "email": "john@example.com",
  "phone": "+1234567890",
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```

#### Update User
```http
PUT /api/users/{id}
Content-Type: application/json

{
  "email": "newemail@example.com",
  "phone": "+9876543210",
  "enabled": true
}
```

#### Change Password
```http
PUT /api/users/{id}/password
Content-Type: application/json

{
  "newPassword": "newSecurePassword456"
}
```

#### Delete User
```http
DELETE /api/users/{id}
```

### Role Management

#### Get All Roles
```http
GET /api/users/roles
```

#### Create Role
```http
POST /api/users/roles
Content-Type: application/json

{
  "name": "ROLE_OPERATOR",
  "description": "Database operator with limited permissions"
}
```

---

## Notifications

### Notification Preferences

#### Get All Preferences
```http
GET /api/notifications/preferences
```

#### Get User Preferences
```http
GET /api/notifications/preferences/{userId}
```

#### Create Notification Preference
```http
POST /api/notifications/preferences
Content-Type: application/json

{
  "user": {
    "id": 1
  },
  "emailEnabled": true,
  "smsEnabled": false,
  "emailAddress": "alerts@example.com",
  "phoneNumber": "+1234567890",
  "alertTypes": "CPU_HIGH,MEMORY_HIGH,CONNECTION_POOL_HIGH",
  "severityLevels": "CRITICAL,WARNING"
}
```

#### Update Notification Preference
```http
PUT /api/notifications/preferences/{id}
Content-Type: application/json

{
  "emailEnabled": true,
  "smsEnabled": true,
  "alertTypes": "CPU_HIGH,MEMORY_HIGH",
  "severityLevels": "CRITICAL"
}
```

#### Delete Preference
```http
DELETE /api/notifications/preferences/{id}
```

### Testing Notifications

#### Test Email
```http
POST /api/notifications/test/email?to=test@example.com
```

#### Test SMS
```http
POST /api/notifications/test/sms?to=+1234567890
```

---

## Dashboard Widgets

#### Get User Widgets
```http
GET /api/widgets/user/{userId}
```

#### Get Visible User Widgets
```http
GET /api/widgets/user/{userId}/visible
```

#### Create Widget
```http
POST /api/widgets
Content-Type: application/json

{
  "user": {
    "id": 1
  },
  "widgetType": "CPU",
  "title": "CPU Usage Monitor",
  "positionX": 0,
  "positionY": 0,
  "width": 4,
  "height": 3,
  "configuration": "{\"refreshInterval\": 5000}",
  "visible": true,
  "sortOrder": 0
}
```

#### Update Widget
```http
PUT /api/widgets/{id}
Content-Type: application/json

{
  "title": "Updated CPU Monitor",
  "visible": true,
  "positionX": 1,
  "positionY": 1
}
```

#### Delete Widget
```http
DELETE /api/widgets/{id}
```

#### Reorder Widgets
```http
POST /api/widgets/user/{userId}/reorder
Content-Type: application/json

[1, 3, 2, 4, 5]
```

---

## Reports & Export

### Export Metrics

#### Export Metrics to PDF
```http
GET /api/reports/metrics/pdf?connectionId=1
```
Downloads: `metrics-report.pdf`

#### Export Metrics to Excel
```http
GET /api/reports/metrics/excel?connectionId=1
```
Downloads: `metrics-report.xlsx`

### Export Queries

#### Export Queries to Excel
```http
GET /api/reports/queries/excel
```
Downloads: `queries-report.xlsx`

### Export Alerts

#### Export Alerts to PDF
```http
GET /api/reports/alerts/pdf
```
Downloads: `alerts-report.pdf`

---

## Backup Monitoring

#### Check Backup Status
```http
GET /api/backup/check/{connectionId}
```

Response:
```json
{
  "id": 1,
  "connectionId": 1,
  "connectionName": "Production DB",
  "lastBackupTime": "2025-12-26T10:30:00",
  "backupType": "FULL",
  "backupStatus": "SUCCESS",
  "backupSizeBytes": 1073741824,
  "backupDurationSeconds": 300,
  "backupLocation": "/backups/prod_20251226.bak",
  "checkedAt": "2025-12-26T22:00:00"
}
```

#### Get Latest Backup Status
```http
GET /api/backup/latest/{connectionId}
```

#### Get Backup History
```http
GET /api/backup/history/{connectionId}
```

#### Get Failed Backups
```http
GET /api/backup/failed
```

#### Get Old Backups
```http
GET /api/backup/old?days=7
```

---

## Replication Monitoring

#### Check Replication Status
```http
GET /api/replication/check/{connectionId}
```

Response:
```json
{
  "id": 1,
  "connectionId": 1,
  "connectionName": "Replica DB",
  "replicationType": "SLAVE",
  "replicationState": "RUNNING",
  "lagSeconds": 5,
  "lagBytes": 10240,
  "masterHost": "master.db.example.com",
  "masterPort": 3306,
  "slaveIORunning": "Yes",
  "slaveSQLRunning": "Yes",
  "lastError": null,
  "lastChecked": "2025-12-26T22:00:00"
}
```

#### Get Latest Replication Status
```http
GET /api/replication/latest/{connectionId}
```

#### Get Replication History
```http
GET /api/replication/history/{connectionId}
```

#### Get Replication Errors
```http
GET /api/replication/errors
```

#### Get High Lag Replicas
```http
GET /api/replication/high-lag?lagThresholdSeconds=60
```

---

## Query Plan Analysis

#### Analyze Query Plan
```http
POST /api/query-plans/analyze?connectionId=1
Content-Type: application/json

{
  "queryText": "SELECT * FROM users WHERE email = 'test@example.com'"
}
```

Response:
```json
{
  "id": 1,
  "queryText": "SELECT * FROM users WHERE email = 'test@example.com'",
  "executionPlan": "Type: ref, Key: idx_email, Rows: 1, Extra: Using where",
  "planType": "EXPLAIN",
  "estimatedCost": 1.2,
  "estimatedRows": 1,
  "usesIndex": true,
  "indexesUsed": "idx_email",
  "hasFullTableScan": false,
  "analyzedAt": "2025-12-26T22:00:00"
}
```

#### Get Query Plans for a Query
```http
GET /api/query-plans/query/{queryId}
```

#### Get Queries with Full Table Scans
```http
GET /api/query-plans/full-table-scans
```

#### Get Queries Without Indexes
```http
GET /api/query-plans/without-indexes
```

#### Get Recent Query Plans
```http
GET /api/query-plans/recent?hours=24
```

---

## Index Recommendations

#### Generate Recommendations
```http
POST /api/index-recommendations/generate/{connectionId}
```

Response:
```json
[
  {
    "id": 1,
    "connectionId": 1,
    "tableName": "users",
    "columnNames": "email",
    "indexType": "BTREE",
    "recommendationReason": "Column frequently used in WHERE clauses",
    "impactScore": 85,
    "affectedQueries": 120,
    "estimatedPerformanceGain": 45.5,
    "createIndexStatement": "CREATE INDEX idx_users_email ON users(email);",
    "status": "PENDING",
    "recommendedAt": "2025-12-26T22:00:00"
  }
]
```

#### Get Recommendations
```http
GET /api/index-recommendations/{connectionId}
```

#### Get Pending Recommendations
```http
GET /api/index-recommendations/{connectionId}/pending
```

#### Apply Recommendation
```http
POST /api/index-recommendations/{id}/apply
```

#### Reject Recommendation
```http
POST /api/index-recommendations/{id}/reject
```

#### Get Applied Recommendations
```http
GET /api/index-recommendations/applied
```

#### Get Recommendations for Table
```http
GET /api/index-recommendations/table/{tableName}
```

---

## Configuration

### Email Notifications

```properties
notification.email.enabled=true
notification.email.from=noreply@dbmonitor.com
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### SMS Notifications (Twilio)

```properties
notification.sms.enabled=true
notification.sms.twilio.account-sid=your-twilio-sid
notification.sms.twilio.auth-token=your-twilio-token
notification.sms.twilio.from-number=+1234567890
```

### InfluxDB Time-Series Database

```properties
influxdb.enabled=true
influxdb.url=http://localhost:8086
influxdb.token=your-influxdb-token
influxdb.org=dbmonitor
influxdb.bucket=metrics
```

### Security

```properties
security.enabled=true
```

---

## Error Responses

All endpoints return standard HTTP status codes:

- `200 OK` - Request successful
- `201 Created` - Resource created
- `400 Bad Request` - Invalid request
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

Error response format:
```json
{
  "timestamp": "2025-12-26T22:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users"
}
```

---

## Rate Limiting

API rate limits (if configured):
- Standard endpoints: 100 requests per minute
- Report generation: 10 requests per minute
- Notification test endpoints: 5 requests per minute

---

## Support

For issues or questions, please refer to:
- Code documentation in source files
- Spring Boot documentation
- Project README.md

**Last Updated:** December 26, 2025

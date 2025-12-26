# Enterprise Features Configuration Guide

This guide provides step-by-step instructions for configuring and enabling all enterprise features in the Database Performance Monitor application.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Email Notifications](#email-notifications)
3. [SMS Notifications](#sms-notifications)
4. [User Authentication](#user-authentication)
5. [InfluxDB Time-Series Database](#influxdb-time-series-database)
6. [Database Support](#database-support)
7. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Minimal Configuration (No Enterprise Features)

```properties
# application.properties
spring.profiles.active=prod
security.enabled=false
notification.email.enabled=false
notification.sms.enabled=false
influxdb.enabled=false
```

### Full Enterprise Configuration

```properties
# application.properties
spring.profiles.active=prod
security.enabled=true
notification.email.enabled=true
notification.sms.enabled=true
influxdb.enabled=true
```

---

## Email Notifications

### Prerequisites

- SMTP server access (Gmail, SendGrid, AWS SES, etc.)
- SMTP credentials

### Gmail Configuration

1. **Enable 2-Factor Authentication** in your Gmail account
2. **Generate an App Password**:
   - Go to Google Account Settings → Security
   - Under "Signing in to Google", select "App passwords"
   - Generate a new app password for "Mail"

3. **Configure in application.properties**:

```properties
# Email Configuration
notification.email.enabled=true
notification.email.from=your-email@gmail.com

# Gmail SMTP Settings
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### SendGrid Configuration

```properties
notification.email.enabled=true
notification.email.from=noreply@yourdomain.com

spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=YOUR_SENDGRID_API_KEY
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### AWS SES Configuration

```properties
notification.email.enabled=true
notification.email.from=noreply@yourdomain.com

spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=YOUR_AWS_SMTP_USERNAME
spring.mail.password=YOUR_AWS_SMTP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Testing Email Notifications

```bash
curl -X POST "http://localhost:8080/api/notifications/test/email?to=test@example.com"
```

---

## SMS Notifications

### Prerequisites

- Twilio account (https://www.twilio.com)
- Phone number verified in Twilio
- Twilio Account SID and Auth Token

### Setup Steps

1. **Create Twilio Account**:
   - Sign up at https://www.twilio.com/try-twilio
   - Verify your phone number

2. **Get Credentials**:
   - Account SID: Found on Twilio Console Dashboard
   - Auth Token: Found on Twilio Console Dashboard
   - Phone Number: Purchase a number or use trial number

3. **Configure in application.properties**:

```properties
# SMS Configuration
notification.sms.enabled=true
notification.sms.twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
notification.sms.twilio.auth-token=your_auth_token_here
notification.sms.twilio.from-number=+1234567890
```

### Testing SMS Notifications

```bash
curl -X POST "http://localhost:8080/api/notifications/test/sms?to=+1234567890"
```

### Trial Account Limitations

- Trial accounts can only send to verified numbers
- Messages include "Sent from your Twilio trial account" prefix
- Upgrade to paid account to remove limitations

---

## User Authentication

### Enabling Security

1. **Update application.properties**:

```properties
security.enabled=true
```

2. **Create Initial Admin User** (via API after first startup):

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "SecurePassword123!",
    "email": "admin@example.com",
    "roles": ["ROLE_ADMIN"]
  }'
```

3. **Create Roles**:

```bash
# Admin role
curl -X POST http://localhost:8080/api/users/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ROLE_ADMIN",
    "description": "Full system access"
  }'

# User role
curl -X POST http://localhost:8080/api/users/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ROLE_USER",
    "description": "Read-only access"
  }'
```

### Password Requirements

- Minimum 8 characters
- Use BCrypt encryption
- Change default passwords immediately

### Access Control

With security enabled:
- `/api/**` - Requires ROLE_USER or ROLE_ADMIN
- `/admin/**` - Requires ROLE_ADMIN
- `/actuator/**` - Requires ROLE_ADMIN
- All other pages require authentication

---

## InfluxDB Time-Series Database

### Prerequisites

- InfluxDB 2.x installed and running
- InfluxDB access token

### Installation

#### Docker Installation

```bash
docker run -d \
  --name influxdb \
  -p 8086:8086 \
  -v influxdb-data:/var/lib/influxdb2 \
  influxdb:2.7
```

#### Manual Installation

Download from: https://portal.influxdata.com/downloads/

### Setup Steps

1. **Access InfluxDB UI**: http://localhost:8086

2. **Create Organization and Bucket**:
   - Organization: `dbmonitor`
   - Bucket: `metrics`
   - Retention: Configure as needed (e.g., 30 days)

3. **Generate API Token**:
   - Go to Data → API Tokens
   - Generate All Access Token or Custom Token with read/write access to `metrics` bucket
   - Copy the token

4. **Configure in application.properties**:

```properties
# InfluxDB Configuration
influxdb.enabled=true
influxdb.url=http://localhost:8086
influxdb.token=YOUR_INFLUXDB_TOKEN_HERE
influxdb.org=dbmonitor
influxdb.bucket=metrics
```

### Verifying Data Collection

After starting the application, check InfluxDB:

```bash
# Query recent data
curl -X POST http://localhost:8086/api/v2/query \
  -H "Authorization: Token YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "from(bucket: \"metrics\") |> range(start: -1h)",
    "org": "dbmonitor"
  }'
```

Or use the InfluxDB UI Data Explorer.

---

## Database Support

### Supported Databases

- MySQL 5.7+
- MariaDB 10.3+
- PostgreSQL 10+
- Microsoft SQL Server 2016+
- Oracle Database 11g+
- H2 (development/testing)

### MySQL/MariaDB Configuration

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dbmonitor?useSSL=false&serverTimezone=UTC
spring.datasource.username=dbmonitor
spring.datasource.password=secure_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### PostgreSQL Configuration

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dbmonitor
spring.datasource.username=dbmonitor
spring.datasource.password=secure_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

### SQL Server Configuration

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=dbmonitor;encrypt=true;trustServerCertificate=true
spring.datasource.username=dbmonitor
spring.datasource.password=secure_password
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### Multi-Database Monitoring

To monitor multiple databases:

1. Use the `/api/connections` endpoints to add connections
2. Each connection will be monitored independently
3. Metrics are tracked per connection ID

---

## Troubleshooting

### Email Notifications Not Working

**Problem**: Test email fails

**Solutions**:
1. Check SMTP credentials
2. Verify firewall allows outbound SMTP traffic (port 587/465)
3. For Gmail: Ensure App Password is used, not regular password
4. Check application logs for detailed error messages
5. Test SMTP connection independently:
   ```bash
   telnet smtp.gmail.com 587
   ```

### SMS Notifications Not Working

**Problem**: Test SMS fails

**Solutions**:
1. Verify Twilio credentials (Account SID, Auth Token)
2. Check phone number format (E.164: +1234567890)
3. For trial accounts: Verify destination number in Twilio console
4. Check Twilio account balance
5. Review Twilio logs in console

### InfluxDB Connection Issues

**Problem**: Metrics not persisting

**Solutions**:
1. Verify InfluxDB is running: `curl http://localhost:8086/health`
2. Check token permissions
3. Verify organization and bucket names
4. Check application logs for connection errors
5. Test connection manually:
   ```bash
   influx ping --host http://localhost:8086
   ```

### Security/Authentication Issues

**Problem**: Cannot access endpoints after enabling security

**Solutions**:
1. Ensure roles are created before users
2. Use correct role names (must start with `ROLE_`)
3. Clear browser cache/cookies
4. Check password encryption
5. Temporarily disable security to verify other functionality

### Performance Issues

**Problem**: Application slow with all features enabled

**Solutions**:
1. Adjust metrics collection interval:
   ```properties
   monitor.refresh.interval-ms=10000  # Increase from 5000
   ```
2. Limit InfluxDB writes with batching
3. Increase JVM heap size:
   ```bash
   java -Xmx2g -jar database-performance-monitor.jar
   ```
4. Use external database instead of H2 for production
5. Review query optimization and indexing

### Backup/Replication Monitoring Issues

**Problem**: Cannot retrieve backup/replication status

**Solutions**:
1. Verify database user has necessary permissions
2. For MySQL: User needs `REPLICATION CLIENT` privilege
3. For SQL Server: User needs `VIEW SERVER STATE` permission
4. For PostgreSQL: User needs `pg_monitor` role
5. Check database-specific requirements in service implementation

---

## Production Checklist

Before deploying to production:

- [ ] Change all default passwords
- [ ] Enable security (`security.enabled=true`)
- [ ] Configure SSL/TLS for HTTPS
- [ ] Set up external database (not H2)
- [ ] Configure InfluxDB with appropriate retention policy
- [ ] Set up email/SMS notifications
- [ ] Configure backup strategy for application data
- [ ] Set appropriate log levels
- [ ] Configure monitoring and alerting
- [ ] Review and adjust connection pool sizes
- [ ] Set up proper firewall rules
- [ ] Configure reverse proxy (nginx, Apache)
- [ ] Enable CORS if needed for external clients
- [ ] Document all configuration changes
- [ ] Test all critical features

---

## Support Resources

- **Application Logs**: `logs/application.log`
- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **InfluxDB Documentation**: https://docs.influxdata.com/
- **Twilio Documentation**: https://www.twilio.com/docs
- **GitHub Issues**: [Report issues](https://github.com/arifgit12/db-monitor/issues)

---

**Last Updated:** December 26, 2025

# PostgreSQL Setup & Backup Guide

## PostgreSQL Configuration

### Prerequisites
- PostgreSQL 12+ installed
- Database created: `CREATE DATABASE notekeeper;`

### Running with PostgreSQL

#### Option 1: Environment Variables
```bash
export SPRING_PROFILES_ACTIVE=postgresql
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=notekeeper
export DB_USER=postgres
export DB_PASSWORD=your_password
mvn spring-boot:run
```

#### Option 2: application-postgresql.yml
Create `src/main/resources/application-postgresql.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/notekeeper
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
```

Then run:
```bash
export SPRING_PROFILES_ACTIVE=postgresql
mvn spring-boot:run
```

## Backup & Restore

### Manual Backup (via UI)

1. Go to **Settings** → **Backup** tab
2. Click **Export Data** to download all data as JSON
3. Click **Import File** to restore from a backup file

⚠️ **Warning**: Import will overwrite existing data!

### Manual Backup (via API)

#### Export
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/backup/export \
  -o backup.json
```

#### Import
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@backup.json" \
  http://localhost:8080/api/v1/backup/import
```

#### List Backups
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/backup/list
```

#### Delete Backup
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  -X DELETE \
  http://localhost:8080/api/v1/backup/delete/backup_20260422_120000.json
```

### Automatic Backup

#### Enable Scheduled Backup
```bash
export BACKUP_AUTO_ENABLED=true
export BACKUP_CRON="0 0 2 * * *"  # Daily at 2 AM
```

#### Cron Format
```
Second Minute Hour Day Month Weekday
0      0      2    *   *     *       = Daily at 2 AM
0      0      *    *   *     *       = Every hour
0      30     8    *   *     MON-FRI = Weekdays at 8:30 AM
```

#### Backup Location
Backups are stored in: `./backups/backup_YYYYMMDD_HHMMSS.json`

### Migration from SQLite to PostgreSQL

1. Export data from SQLite:
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/v1/backup/export \
     -o backup.json
   ```

2. Stop the application

3. Switch to PostgreSQL (set environment variables)

4. Start the application (schema will be auto-created)

5. Import data:
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
     -F "file=@backup.json" \
     http://localhost:8080/api/v1/backup/import
   ```

## Troubleshooting

### PostgreSQL Connection Issues
- Check if PostgreSQL is running: `pg_isready`
- Verify database exists: `psql -U postgres -l`
- Check credentials in environment variables

### Backup Import Fails
- Ensure backup file is valid JSON
- Check file size (large files may timeout)
- Verify foreign key constraints don't conflict

### Automatic Backup Not Working
- Check logs for scheduling messages
- Verify `BACKUP_AUTO_ENABLED=true`
- Validate cron expression format

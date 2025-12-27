-- SQL Server Schema for Spring Security Remember Me functionality
-- This table stores persistent login tokens for "Remember Me" feature

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[persistent_logins]') AND type in (N'U'))
BEGIN
    CREATE TABLE persistent_logins (
        username VARCHAR(64) NOT NULL,
        series VARCHAR(64) NOT NULL PRIMARY KEY,
        token VARCHAR(64) NOT NULL,
        last_used DATETIME2 NOT NULL
    );
    
    -- Create index on username for faster lookup
    CREATE INDEX IX_persistent_logins_username ON persistent_logins(username);
END

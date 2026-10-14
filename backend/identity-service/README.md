# Identity Service

Identity is the authentication and account boundary for marketplace users, vendors, and operators. This foundation keeps the service intentionally small so the authentication provider and persistence model can be selected without coupling the other services to them.

## Run

```bash
mvn -pl identity-service spring-boot:run
```

The default port is `8081`; set `SERVER_PORT` to override it. Health: `GET /actuator/health`.

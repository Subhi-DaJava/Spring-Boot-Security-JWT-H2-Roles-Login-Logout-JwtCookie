### Run with command: mvn spring-boot:run or Run IDE, check H2 database with url: http://localhost:8080/h2-ui -> jdbc:h2:./testdb

### Exécuter sur la console de h2 pour la première fois : 
INSERT INTO roles(name) VALUES('ROLE_USER');
INSERT INTO roles(name) VALUES('ROLE_MODERATOR');
INSERT INTO roles(name) VALUES('ROLE_ADMIN');

### Add the user 
#### Postman: Post  -> http://localhost:8080/api/auth/signup
{
"username": "modUyghur",
"email": "mod@uyghurjava.com",
"password": "12345678",
"role": ["mod", "user"]
}

{
"username": "userUyghur",
"email": "user@uyghurjava.com",
"password": "12345678",
"role": ["user"]
}

{
"username": "adminUyghur",
"email": "admin@uyghurjava.com",
"password": "12345678",
"role": ["admin","user"]
}

If success: {"message": "User registered successfully!"} 

### In DB: Table=users:
####            id         email                  password                                         username     
####            1 mod@uyghurjava.com    $2a$10$BC6OBR7BiRLT1P7Q1evxUuMsyCTaHGJJLqXTFXT.bA3FdrWTA6zRa   modUyghur
####            2 user@uyghurjava.com   $2a$10$DOcc0cFu5QDu94GN6ef88..UugSx9AmXcE8xHg7hgS8/ydZILvSb.   userUyghur
####            3 admin@uyghurjava.com  $2a$10$1KZATjOD63z2LriZVSFhy.JIwRWQvmidjZUDUx8Hh49N/zIcWP/ha   adminUyghur

#### Collection: all requests (in Postman folder)

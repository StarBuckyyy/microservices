## Démarrage
**Important : Nécessite Docker et Docker Compose sur la machine**
Si vous ne les avez pas, installez-les : https://www.docker.com/products/docker-desktop/
**Nécessite k6 pour les tests de charge**
sudo apt-get update
sudo apt-get install k6

**Les conteneurs de ce projet utilisent les ports 8080 et 5432, vérifier qu'ils ne soient pas en route :**
docker ps --filter "publish=8080"
docker ps --filter "publish=5432"

**Si un conteneur est trouvé, l'arrêter avec son nom**
docker stop <nom_du_conteneur>


### Monolithe (Phase 1)
Dans votre terminal, dans /microservices :
docker-compose -f monolith/docker-compose.yml up --build -d
(attendre environ 30 secondes)

### Microservices (Phase 2)
Dans votre terminal, dans /microservices :
docker compose -f docker-compose-microservices.yml up --build -d
(attendre environ 1 minute)

**Enfin, pour accéder à l'application, vous pouvez accéder à l'interface web au :**
http://localhost:8080


### Tests de Charge
Une fois que tout est build, tout est fonctionnel, on peut lancer un test de charge (pour phase 1 ou phase 2) :
k6 run k6-tests/microservices-test.js
k6 run k6-tests/monolith-test.js

### Prometheus + grafana (phase 1 ou 2) 
Une fois que tout est fonctionnel, on peut le vérifier à l'adresse :
http://localhost:9090/targets 
D'ici on voit que tout est vert, c'est-à-dire que tous les microservices sont lancés, et UP. 

Pour observer les graphes que l'on voit dans mon rapport dans la partie 5, on peut aller à l'adresse : 
http://localhost:3000
On se connecte avec l'identifiant admin, et le mot de passe admin
Dans le menu déroulant à gauche, on clique sur "Add new connection", on cherche "Prometheus", on l'ajoute en haut à droite. Une adresse est demandé, on met : 
"http://prometheus:9090", puis "Save and test". 
Enfin, dans le menu déroulant, on va dans "Dashboard", en haut à droite : "Import", puis on met l'ID : 4701 (dashboard importé). Une source prometheus est demandée, on sélectionne le prometheus. Et on load, on arrive sur le dashboard avec des données vides, il suffit de sélectionner en haut à droite "Last 5 minutes" et on voit des données commencer à apparaître.



**Pour accéder à Swagger-ui pour voir la documentation API, chaque service l'a**
Pour account-service, aller à : 
http://localhost:8081/swagger-ui/index.html
Pour wallet-service, aller à : 
http://localhost:8082/swagger-ui/index.html
Pour order-service, aller à : 
http://localhost:8083/swagger-ui/index.html



**Une fois l'évaluation terminée, vous pouvez arrêter les conteneurs, supprimer le réseau et effacer toutes les données avec la commande :**
Phase 1 : 
docker compose -f monolith/docker-compose.yml down -v

  
Phase 2 : 
docker compose -f docker-compose-microservices.yml down -v

  





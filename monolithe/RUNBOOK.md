**Important : Nécessite Docker et Docker Compose sur la machine**
Si vous ne les avez pas, installez-les : https://www.docker.com/products/docker-desktop/

**Les conteneurs de ce projet utilisent les ports 8080 et 5432, vérifier qu'ils ne soient pas en route :**
docker ps --filter "publish=8080"
docker ps --filter "publish=5432"

**Si un conteneur est trouvé, l'arrêter avec son nom**
docker stop <nom_du_conteneur>


**Lancement de l'applciation :**
docker compose up --build -d 

**Enfin, pour accéder à l'application, vous pouvez accéder à l'interface web au :**
http://localhost:8080

**Si vous voulez check l'endpoint de santé, vous pouvez vous rendre à l'adresse :**
http://localhost:8080/actuator/health   (devrait retourner "status" : up)


**Pour check la base de données, vous avez deux solutions :**
1. check les tables séparément aux adresses : 
http://localhost:8080/users - Tables User
http://localhost:8080/accounts - Table Account
http://localhost:8080/wallets - Table Wallet
http://localhost:8080/transactions - Table Transaction
http://localhost:8080/orders - Table Order

2.check la base de données sur un clent graphique (DBeaver, DataGrip, etc.) avec les paramètres suivants : 
-Hôte : localhost
-Port : 5432
-BDD : brokerx
-Utilisateur : brokerx_user
-Mot de passe : brokerx_pass


**Une fois l'évaluation terminée, vous pouvez arrêter les conteneurs, supprimer le réseau et effacer toutes les données avec la commande :**
docker-compose down -v



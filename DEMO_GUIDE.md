**Ce guide vous accompagnera à travers un scénario complet pour démontrer les fonctionnalités clés de l'application BrokerX**

Après avoir check les tables de la base de données avec les deux méthodes fournies dans le runbook, vous allez pouvoir ajouter un utilisateur. 
**Rendez vous à l'adresse : http://localhost:8080 ***

D'ici vous pouvez vous connecter, ou vous inscrire. En décidant de vous inscrire, vous remplissez un formulaire, et l'utilisateur et le compte (en état Pending) sont créés. Une fois que vous validerez le compte avec la vérification OTP, le wallet est aussi créé. 

Vous pouvez alors revenir sur la page de connexion et vous connecter avec votre compte créé. 
Ici, vous aurez un code OTP encore pour le MFA (simulé). Puis vous serez redirigé vers le dashboard.
D'ici, vous verrez en plein milieu de votre écran ainsi qu'en haut à droite, votre solde. Vous pouvez cliquer sur le bouton "Déposer des fonds". Une fois que vous saisissez le montant à déposer, le service de paiement simulé effectuera la transaction sur le compte. Ainsi, vous verrez le montant du wallet augmenter, et la transaction apparaitre dans la table. 

Enfin, vous pouvez créer un ordre, avec le bouton "Placer un ordre". Depuis cette page, vous formulez une demande d'ordre avec le Symbole, le Côté, le type d'ordre, la quantité, et le prix (si type LIMIT). Vous verrez ensuite les Statistiques se mettre à jour plus bas, avec l'ordre qui apparaitra dans "Mes ordres" ainsi que dans la table Order.
Note : Les ordres dépassant le montant **disponible** sur le Wallet ne seront pas process.
Ensuite, vous pouvez modifier les ordres, ou les annuler. 
Vous pouvez accéder à la page web suivante pour voir les logs d'audit : 
http://localhost:8080/audit-viewer.html

Finalement, les 5 UC Must ont été implementés, et ont été démontrés avec ce guide de démonstration.
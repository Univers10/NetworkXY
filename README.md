# NetworkXY - Application de Gestion de Réseau Domestique

Application Android pour gérer et visualiser les objets connectés dans un appartement.

## Télécharger l'APK

[![Télécharger APK](https://img.shields.io/badge/Télécharger-APK-green?style=for-the-badge&logo=android)](https://github.com/Univers10/NetworkXY/releases/latest/download/NetworkXY.apk)

## Fonctionnalités

### Menu Principal

Le menu est organisé en deux sous-menus et des options directes :

#### Sous-menu Objets
- **Visualisation** : Mode par défaut pour voir le réseau
- **Ajouter objet** : Long-click sur l'écran pour ajouter un objet connecté
- **Ajouter connexion** : Drag d'un objet vers un autre pour créer une connexion
- **Modifier** : Déplacer les objets et ajuster la courbure des connexions

#### Sous-menu Actions
- **Choisir le plan** : Sélection entre 4 plans d'appartement
- **Sauvegarder** : Sauvegarde le réseau dans la mémoire interne
- **Charger** : Charge un réseau précédemment sauvegardé
- **Envoyer par mail** : Partage le réseau sous forme de capture d'écran
- **Réinitialiser** : Efface tous les objets et connexions

#### Options directes
- **Filtrer** : Filtrer les appareils par nom, statut ou type de connexion
- **Statistiques** : Tableau de bord du réseau (nombre d'appareils, connexions, statuts)
- **Langue / Language** : Basculer entre Français et Anglais

### Types d'Appareils
Chaque type d'appareil est représenté par une icône dédiée :
- **Routeur** : Icône routeur
- **TV** : Icône téléviseur
- **Ordinateur** : Icône PC
- **Smartphone** : Icône téléphone
- **Tablette** : Icône tablette
- **Imprimante** : Icône imprimante
- **Enceinte** : Icône haut-parleur
- **Caméra** : Icône caméra
- **Autre** : Icône générique

### Objets Connectés
- Ajout par long-click avec étiquette personnalisable
- 7 couleurs disponibles : Rouge, Vert, Bleu, Orange, Cyan, Magenta, Noir
- Statut : En ligne / Hors ligne (indicateur coloré sur chaque nœud)
- Déplacement par drag & drop (les connexions suivent)
- Menu contextuel (long-click) : modifier ou supprimer

### Connexions
- Création par drag entre deux objets
- 4 types de connexion : WiFi, Ethernet, Bluetooth, USB
- Étiquette affichée au milieu de la connexion
- Courbure ajustable en mode édition (drag sur l'étiquette)
- Épaisseur et couleur personnalisables
- Pas de connexions multiples ni de boucles
- Menu contextuel : modifier étiquette, couleur, épaisseur ou supprimer

### Plans d'Appartement
- 4 plans réels d'appartement disponibles (Plan 1, Plan 2, Plan 3, Plan 4)
- Affichage en **taille réelle** (pas de redimensionnement)
- Défilement horizontal et vertical avec deux doigts
- Zoom/dézoom par pincement (pinch)
- Bouton de réinitialisation du zoom

### Guide de Navigation
Au premier lancement, un guide interactif s'affiche pour expliquer les gestes :
- Défilement avec deux doigts
- Zoom/dézoom par pincement
- Réinitialisation du zoom

### Filtres
- Recherche par nom d'appareil
- Filtre par statut (En ligne / Hors ligne)
- Filtre par type de connexion (WiFi, Ethernet, Bluetooth, USB)

### Statistiques Réseau
- Nombre total d'appareils
- Nombre d'appareils en ligne / hors ligne
- Nombre total de connexions

## Architecture

### Pattern MVVM
- **Model** : `Graph`, `NetworkObject`, `Connection`, `ObjectColor`, `NodeType`, `NodeStatus`, `ConnectionType`
- **View** : `NetworkCanvas` (vue personnalisée avec Canvas)
- **ViewModel** : `NetworkViewModel` (gestion d'état avec LiveData)

### Structure du Projet
```
fr.istic.mob.networkXY/
├── model/              # Classes de données (Graph, NetworkObject, Connection, enums)
├── viewmodel/          # ViewModel MVVM
├── view/               # Vue personnalisée NetworkCanvas
├── ui/                 # MainActivity et dialogues
├── repository/         # Sauvegarde/chargement
└── utils/              # Utilitaires (screenshots)
```

## Technologies Utilisées
- **Kotlin** : Langage principal
- **Android Canvas** : Dessin 2D personnalisé
- **ViewModel & LiveData** : Architecture MVVM
- **Kotlinx Serialization** : Sauvegarde JSON
- **Coroutines** : Opérations asynchrones
- **FileProvider** : Partage de fichiers
- **Material Design 3** : Composants UI (MaterialToolbar, MaterialButton, BottomSheetDialog, Chips)
- **SharedPreferences** : Persistance des préférences utilisateur
- **ScaleGestureDetector** : Gestion du pinch-to-zoom
- **BitmapFactory** : Chargement des images de plans d'appartement

## Internationalisation
- Français (par défaut)
- Anglais
- Changement de langue dynamique via le menu

## Gestion de l'Orientation
Le réseau est préservé lors des changements d'orientation grâce au ViewModel qui survit aux changements de configuration.

## Classes Android Utilisées
- `android.graphics.Path` : Tracé des connexions
- `android.graphics.PathMeasure` : Calcul du milieu des connexions
- `android.graphics.RectF` : Limites des objets
- `android.graphics.Canvas` : Dessin 2D
- `android.graphics.BitmapFactory` : Chargement des plans d'appartement
- `android.view.View` : Vue personnalisée
- `android.view.ScaleGestureDetector` : Zoom par pincement
- `GestureDetector` : Détection des long-clicks

## Installation
1. Ouvrir le projet dans Android Studio
2. Synchroniser Gradle
3. Lancer l'application sur un émulateur ou appareil Android (API 24+)

## Utilisation

### Navigation sur le Plan
- **Défiler** : Glisser avec deux doigts
- **Zoomer/Dézoomer** : Pincer avec deux doigts
- **Réinitialiser le zoom** : Appuyer sur le bouton en bas de l'écran

### Ajouter un Objet
1. Menu > Objets > Ajouter objet
2. Long-click sur l'écran
3. Entrer le nom, choisir le type d'appareil et la couleur

### Créer une Connexion
1. Menu > Objets > Ajouter connexion
2. Poser le doigt sur un objet
3. Glisser vers un autre objet
4. Relâcher sur l'objet cible
5. Entrer le nom et le type de connexion

### Modifier un Objet/Connexion
1. Menu > Objets > Modifier
2. Long-click sur l'objet ou la connexion
3. Modifier les propriétés ou supprimer

### Courber une Connexion
1. Menu > Objets > Modifier
2. Drag sur l'étiquette de la connexion
3. La courbure suit le mouvement du doigt

### Choisir un Plan d'Appartement
1. Menu > Actions > Choisir le plan
2. Sélectionner parmi les 4 plans disponibles

## Auteurs
Joel ATTITSO / Gaelle SEKONGO
Master 2 MIAGE - 2025/2026

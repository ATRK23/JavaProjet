# JavaProjet

Application Client/Serveur en Java capable de gêrer un stock de différentes données dynamiquement entre plusieurs machines distantes et un serveur central

## Prerequis
- JDK 8+

## Compilation
```bash
javac Machine.java Slave.java Executor.java Test.java
```

## Lancement manuel (sans Test)
1. Demarrer une ou plusieurs machines (port + ressources sous la forme `3A/2B/0D`) :
   ```bash
   java Machine 30000 3A/2B
   java Machine 30001 1A/5B
   ```
2. Lancer l'executor en lui passant les reactions et les adresses machines :
   ```bash
   java Executor --reaction "3A+2B->C" --machine localhost:30000 --machine localhost:30001
   ```
   (remplacer ```localhost``` par l'adresse distance si nécessaire)

## Lancement automatique via `Test`
`Test` utilise `ProcessBuilder` pour demarrer les JVM necessaires sur la meme machine.

```
java Test --machine "30000 3A/2B" --machine "30001 1A" --executor "--reaction 3A+2B->C --machine localhost:30000 --machine localhost:30001"
```

- `--machine "<port> <ressources>"` peut etre repete; les arguments sont passes tels quels a `Machine`.
- `--executor "<args>"` regroupe toutes les options de l'executor dans une seule chaine (les espaces sont conserves).
- Les sorties standard/erreur des processus sont heritees pour suivre facilement les logs.
- A la fin de l'execution de l'executor, les processus Machine sont detruits.

## Format des reactions et ressources
- Ressources initiales machine : '```<NB><LETTRE>```' séparées par des `/`
   
   Exemple : `1A/3B/1D`
- Reaction cote executor : notation compacte `2A+1B->C` (les `1` sont optionnels, les espaces sont tolerees).

## Journalisation

## Documentation
- Utilisation de JavaDoc
class Color {
  String ANSI;
  boolean x = true;  // franchissable (on peut marcher dessus)
  boolean t = false; // cette case peut rediriger vers une autre map (= porte ou suite d'un couloir)
  boolean i = false; // cette case est interactive

  // données par rapport à la "téléportation"
  // c'est comme ça qu'on appelle une case
  // qui peut rediriger le joueur vers une autre map
  String toMap; // la map ciblée
  int movX; // le déplacement en X nécessaire pour sortir de la map et accéder à la suivante prédéfinie
  int movY; // le déplacement en Y nécessaire pour sortir de la map et accéder à la suivante prédéfinie
  int toX; // la position en X de la case sur laquelle faire apparaître le joueur
  int toY; // la position en Y de la case sur laquelle faire apparaître le joueur
}
/**
 * Dialog décrit un message appartenant à un dialogue.
 * Ici, un dialogue peut faire intervenir un nombre illimité de narrateurs.
 * 
 * La couleur associée obtiendra, lors de l'initilisation, une métadonnée particulière,
 * un booléan, précisant si elle est interactive ou non.
 * La touche d'interaction apparaîtra alors quand le joueur est à proximité,
 * mais si les conditions ne sont pas satisfaites (map correspondante ou progression insuffisante),
 * alors un texte prédéfini sera affiché à la place, du genre "Je veux pas te parler maintenant".
 */
class Dialog {
  // Ce message appartient à un groupe unique. Les mesages de ce groupe vont s'enchainer.
  int group; 
  int colorIndex; // Le numéro de la couleur interactive (= le numéro unique du pnj)
  String map; // Un dialogue peut n'être accessible que dans une map particulière (devant-la-bibliotheque par exemple), `null` s'il n'y a pas de condition
  String narratorName; // le nom de la personne qui parle
  String text; // le message
  boolean isRandom; // `true` si le message appartient à un groupe où le texte doit être pris aléatoirement, sans suite

  void setMap(String v) {
    map = v.length() == 0 ? null : v;
  }
}
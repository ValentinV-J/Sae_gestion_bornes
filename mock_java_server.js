/**
 * mock_java_server.js — Simulateur du Serveur Central Java
 *
 * Simule exactement les réponses du ThreadServer.java de Valentin.
 * Protocole : commandes texte séparées par espaces, une par ligne.
 *
 * Usage : node mock_java_server.js
 */

const net = require('net');
const PORT = 9000;

function traiter(message) {
  console.log(`\n📨 Reçu : "${message}"`);
  const parts = message.split(' ');
  const cmd   = parts[0];

  switch (cmd) {

    // Badge livreur → toujours accepté
    case 'BADGE_RFID':
      console.log(`   Badge ${parts[2]} → Livreur accepté`);
      return 'OK RFID Martin Jean';

    // Demande d'un casier libre pour dépôt → casier 1
    case 'DEMANDE_DEPOT':
      console.log(`   Casier libre trouvé : 1`);
      return 'OK CASIER 1';

    // Code de retrait → accepté sauf "0000" (pour tester ERR)
    case 'CODE_RETRAIT':
      if (parts[2] === '0000') {
        console.log(`   Code invalide : ${parts[2]}`);
        return 'ERR code_retrait invalide';
      }
      console.log(`   Code ${parts[2]} valide → casier 2`);
      return 'OK CASIER 2';

    // Casier fermé (fin dépôt ou retrait)
    case 'CASIER_FERME':
      console.log(`   ✅ Casier ${parts[2]} fermé (${parts[3]})`);
      return 'OK';

    // Problème d'ouverture → propose un casier alternatif
    case 'PROBLEME_OUVERTURE':
      console.log(`   ⚠️  Erreur ouverture casier ${parts[2]} → casier 3 alternatif`);
      return 'OK CASIER 3';

    // Timeout buzzer
    case 'BUZZER_TIMEOUT':
      console.log(`   🚨 Buzzer timeout casier ${parts[2]} (${parts[3]})`);
      return 'OK';

    default:
      console.log(`   ❓ Commande inconnue : ${cmd}`);
      return 'ERR commande_inconnue';
  }
}

const serveur = net.createServer((socket) => {
  const addr = `${socket.remoteAddress}:${socket.remotePort}`;
  console.log(`\n✅ Connexion : ${addr}`);

  let buffer = '';
  socket.on('data', (data) => {
    buffer += data.toString();
    let idx;
    while ((idx = buffer.indexOf('\n')) !== -1) {
      const line = buffer.slice(0, idx).trim();
      buffer = buffer.slice(idx + 1);
      if (!line) continue;
      const rep = traiter(line);
      socket.write(rep + '\n');
      console.log(`   ← Envoyé : "${rep}"`);
    }
  });

  socket.on('end',   () => console.log(`\n🔌 Déconnexion : ${addr}`));
  socket.on('error', (e) => console.error(`❌ Erreur : ${e.message}`));
});

serveur.listen(PORT, '0.0.0.0', () => {
  console.log('╔══════════════════════════════════════════╗');
  console.log('║   🖥️  Mock Serveur Java — TCP port 9000   ║');
  console.log('║   En attente de l\'ESP32...               ║');
  console.log('╚══════════════════════════════════════════╝');
  console.log('\nProtocole (ThreadServer.java de Valentin) :');
  console.log('  BADGE_RFID <borne> <rfid>          → OK RFID <nom> <prenom>');
  console.log('  DEMANDE_DEPOT <borne>               → OK CASIER <N>');
  console.log('  CODE_RETRAIT <borne> <code>         → OK CASIER <N> | ERR ...');
  console.log('  CASIER_FERME <borne> <N> depot|retrait → OK');
  console.log('  PROBLEME_OUVERTURE <borne> <N> <taille> <ctx> → OK CASIER <alt>');
  console.log('  BUZZER_TIMEOUT <borne> <N> depot|retrait → OK');
});

import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, deleteDoc, serverTimestamp, Timestamp } from 'firebase/firestore';
let environment;
const installation = '12345678-1234-1234-1234-123456789abc';
const token = 'fixture-fcm-token-at-least-twenty-characters';
before(async () => {
  environment = await initializeTestEnvironment({ projectId: 'demo-astro-push', firestore: {
    host: '127.0.0.1', port: 8085, rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8'),
  } });
});
beforeEach(async () => environment.clearFirestore());
after(async () => environment?.cleanup());
const device = (db, user = 'alice', id = installation) => doc(db, 'fcm_tokens', user, 'devices', id);
test('somente o dono autenticado cadastra, consulta e remove o próprio dispositivo', async () => {
  const alice = environment.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(device(alice), { token, updated_at: serverTimestamp() }));
  await assertSucceeds(getDoc(device(alice)));
  await assertFails(getDoc(device(environment.authenticatedContext('bob').firestore())));
  await assertFails(deleteDoc(device(environment.authenticatedContext('bob').firestore())));
  await assertFails(getDoc(device(environment.unauthenticatedContext().firestore())));
  await assertSucceeds(deleteDoc(device(alice)));
});
test('não aceita campos extras, token vazio, timestamp arbitrário ou instalação inválida', async () => {
  const db = environment.authenticatedContext('alice').firestore();
  await assertFails(setDoc(device(db), { token, updated_at: serverTimestamp(), role: 'admin' }));
  await assertFails(setDoc(device(db), { token: '', updated_at: serverTimestamp() }));
  await assertFails(setDoc(device(db), { token, updated_at: Timestamp.fromMillis(1000) }));
  await assertFails(setDoc(device(db, 'alice', 'hardware-id'), { token, updated_at: serverTimestamp() }));
  await assertFails(setDoc(device(db, 'bob'), { token, updated_at: serverTimestamp() }));
});
test('dois dispositivos são independentes e a renovação não remove o segundo', async () => {
  const db = environment.authenticatedContext('alice').firestore();
  const second = device(db, 'alice', 'abcdefab-1234-1234-1234-123456789abc');
  await assertSucceeds(setDoc(device(db), { token, updated_at: serverTimestamp() }));
  await assertSucceeds(setDoc(second, { token: token + '-second', updated_at: serverTimestamp() }));
  await assertSucceeds(setDoc(device(db), { token: token + '-renewed', updated_at: serverTimestamp() }));
  await assertSucceeds(deleteDoc(device(db)));
  const saved = await assertSucceeds(getDoc(second));
  if (!saved.exists()) throw new Error('O segundo dispositivo foi removido indevidamente');
});

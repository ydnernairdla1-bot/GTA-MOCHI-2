const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_vice_01";
const BOB_UID = "bob_vice_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCareerPayload(uid, now) {
  return {
    userId: uid,
    callsign: "Vercetti",
    cash: 1500,
    respect: 120,
    highScore: 3400,
    missionsCompleted: 1,
    vehiclesStolen: 3,
    maxWantedSurvived: 2,
    unlockedWeapons: ["FISTS", "PISTOL"],
    equippedWeapon: "PISTOL",
    ownedVehicles: ["SEDAN"],
    equippedVehicle: "SEDAN",
    armorLevel: 1,
    nitroLevel: 0,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write career profiles or runs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validCareerPayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("runs").get());
});

test("Owner can create and partially update career profile while rejecting ghost fields and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userRef.set(validCareerPayload(ALICE_UID, created)));
  await assertSucceeds(userRef.get());

  // Partial update mirroring Kotlin repository update
  await assertSucceeds(
    userRef.update({
      cash: 4200,
      respect: 350,
      missionsCompleted: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    userRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject createdAt tampering
  await assertFails(
    userRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative cash
  await assertFails(
    userRef.update({
      cash: -50,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId id injection
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validCareerPayload(BOB_UID, created),
      id: BOB_UID,
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice career or runs", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(validCareerPayload(ALICE_UID, created));
    await adminDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_1").set({
      userId: ALICE_UID,
      title: "Hotwire Run",
      outcome: "PASSED",
      cashEarned: 2500,
      wantedPeak: 2,
      createdAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ cash: 999999, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("runs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_1").get());
});

test("Owner can create and list mission runs; updates and invalid outcomes are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 2000);
  const runsCol = aliceDb.collection("users").doc(ALICE_UID).collection("runs");

  // Empty list query succeeds on user-scoped subcollection
  await assertSucceeds(runsCol.orderBy("createdAt", "desc").limit(20).get());

  await assertSucceeds(
    runsCol.doc("run_1").set({
      userId: ALICE_UID,
      title: "Syndicate Shakedown",
      outcome: "PASSED",
      cashEarned: 3500,
      wantedPeak: 3,
      createdAt: created,
    })
  );

  await assertSucceeds(runsCol.orderBy("createdAt", "desc").limit(20).get());

  // Immutable log cannot be updated
  await assertFails(runsCol.doc("run_1").update({ cashEarned: 999999 }));

  // Invalid outcome rejected
  await assertFails(
    runsCol.doc("run_2").set({
      userId: ALICE_UID,
      title: "Bad Run",
      outcome: "CHEATED",
      cashEarned: 100,
      wantedPeak: 1,
      createdAt: created,
    })
  );

  // Future timestamp rejected
  await assertFails(
    runsCol.doc("run_3").set({
      userId: ALICE_UID,
      title: "Future Run",
      outcome: "PASSED",
      cashEarned: 100,
      wantedPeak: 1,
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

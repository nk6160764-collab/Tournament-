const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";
const ADMIN_EMAIL = "nk6160764@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function buildPlayerProfilePayload(uid, email = "player@example.com") {
  const now = new Date(Date.now() - 2000);
  return {
    uid,
    displayName: "Ali Khan",
    email,
    inGameName: "PK_Shadow",
    freeFireUid: "198273645",
    whatsappNumber: "03001234567",
    teamName: "Karachi Wolves",
    role: "player",
    matchesPlayed: 0,
    totalWins: 0,
    matchPoints: 0,
    killPoints: 0,
    totalPoints: 0,
    walletBalancePkr: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function buildLeaderboardPayload(uid) {
  return {
    userId: uid,
    inGameName: "PK_Shadow",
    freeFireUid: "198273645",
    teamName: "Karachi Wolves",
    matchesPlayed: 0,
    totalWins: 0,
    matchPoints: 0,
    killPoints: 0,
    totalPoints: 0,
    updatedAt: new Date(Date.now() - 2000),
  };
}

function buildTournamentPayload(createdBy) {
  const now = new Date(Date.now() - 2000);
  return {
    title: "PK Squad Championship",
    mode: "Squad",
    status: "upcoming",
    mapName: "Bermuda",
    entryFeePkr: 50,
    prizePoolPkr: 1750,
    firstPrizePkr: 1000,
    secondPrizePkr: 500,
    thirdPrizePkr: 250,
    matchTime: "Tonight 9:00 PM PKT",
    totalSlots: 48,
    registeredCount: 0,
    rules: "Mobile players only. No hacks or teaming.",
    roomId: "",
    roomPassword: "",
    winnerName: "",
    winnerUid: "",
    createdBy,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(unauthDb.collection("announcements").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(buildPlayerProfilePayload(ALICE_UID)));
});

test("Player signup writes /users/{uid} and /leaderboard/{uid} securely and blocks role escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();

  // Valid player signup succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildPlayerProfilePayload(ALICE_UID, "alice@gmail.com")));
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(buildLeaderboardPayload(ALICE_UID)));

  // Role escalation on signup must fail
  const escalated = buildPlayerProfilePayload(BOB_UID, "bob@gmail.com");
  escalated.role = "admin";
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(escalated));

  // Cross-user profile read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ inGameName: "Hacked" }));

  // Player cannot tamper with own points or role via update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPoints: 9999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      totalPoints: 9999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player CAN update allowed profile fields (IGN, FF UID, Team, WhatsApp)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      inGameName: "PK_Legend",
      freeFireUid: "99887766",
      teamName: "Lahore Lions",
      whatsappNumber: "03111112233",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofDb.collection("tournaments").doc("t1").set(buildTournamentPayload("spoof_uid"))
  );
});

test("Verified admin can create, update, and delete tournaments and announcements", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("tournaments").doc("t_main").set(buildTournamentPayload(ADMIN_UID)));
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_main").update({
      roomId: "GZPK-100",
      roomPassword: "777",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("a1").set({
      title: "Match Live",
      message: "Room ID posted for PK Squad Championship!",
      category: "tournament",
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Regular player cannot create or delete tournaments
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  await assertFails(aliceDb.collection("tournaments").doc("t_fake").set(buildTournamentPayload(ALICE_UID)));
  await assertFails(aliceDb.collection("tournaments").doc("t_main").delete());
});

test("Atomic tournament join enforces entryFeePkr wallet deduction and blocks unbacked tampering", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("tournaments").doc("t_join").set(buildTournamentPayload(ADMIN_UID)));

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildPlayerProfilePayload(ALICE_UID, "alice@gmail.com")));

  // Admin credits 200 PKR after verifying deposit
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).update({
    walletBalancePkr: 200,
    updatedAt: new Date(Date.now() - 1500),
  }));

  const now = new Date(Date.now() - 1000);
  const regPayload = {
    tournamentId: "t_join",
    tournamentTitle: "PK Squad Championship",
    tournamentMode: "Squad",
    tournamentStatus: "upcoming",
    matchTime: "Tonight 9:00 PM PKT",
    mapName: "Bermuda",
    entryFeePkr: 50,
    prizePoolPkr: 1750,
    userId: ALICE_UID,
    inGameName: "PK_Shadow",
    freeFireUid: "198273645",
    teamName: "Karachi Wolves",
    slotNumber: 1,
    matchPoints: 0,
    killPoints: 0,
    totalPoints: 0,
    placement: 0,
    createdAt: now,
    updatedAt: now,
  };

  // Tampering registeredCount alone without batch registrations must fail
  await assertFails(
    aliceDb.collection("tournaments").doc("t_join").update({
      registeredCount: 1,
      updatedAt: now,
    })
  );

  // Joining a 50 PKR tournament WITHOUT deducting walletBalancePkr must fail
  const unpaidBatch = aliceDb.batch();
  unpaidBatch.set(aliceDb.collection("tournaments").doc("t_join").collection("registrations").doc(ALICE_UID), regPayload);
  unpaidBatch.set(aliceDb.collection("users").doc(ALICE_UID).collection("registrations").doc("t_join"), regPayload);
  unpaidBatch.update(aliceDb.collection("tournaments").doc("t_join"), { registeredCount: 1, updatedAt: now });
  await assertFails(unpaidBatch.commit());

  // Atomic join WITH 50 PKR wallet deduction (200 -> 150) & walletTransactions debit succeeds
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("tournaments").doc("t_join").collection("registrations").doc(ALICE_UID), regPayload);
  batch.set(aliceDb.collection("users").doc(ALICE_UID).collection("registrations").doc("t_join"), regPayload);
  batch.update(aliceDb.collection("tournaments").doc("t_join"), {
    registeredCount: 1,
    updatedAt: now,
  });
  batch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletBalancePkr: 150,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("users").doc(ALICE_UID).collection("walletTransactions").doc("tx_join_1"), {
    userId: ALICE_UID,
    type: "tournament_entry",
    direction: "debit",
    amountPkr: 50,
    title: "Joined: PK Squad Championship",
    subtitle: "Bermuda • Slot #1",
    referenceId: "t_join",
    status: "completed",
    balanceAfterPkr: 150,
    createdAt: now,
  });
  await assertSucceeds(batch.commit());

  // Kotlin queries succeed
  await assertSucceeds(aliceDb.collection("tournaments").orderBy("createdAt", "desc").get());
  await assertSucceeds(aliceDb.collection("leaderboard").orderBy("totalPoints", "desc").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("registrations").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("walletTransactions").orderBy("createdAt", "desc").get());

  // Bob cannot list Alice's private registrations or wallet transactions
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("registrations").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("walletTransactions").get());
});

test("Wallet deposits, withdrawals, self-credit prevention, and admin verification work securely", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildPlayerProfilePayload(ALICE_UID, "alice@gmail.com")));

  const now = new Date(Date.now() - 1000);

  // Player CANNOT increase their own walletBalancePkr or forge a credit transaction
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ walletBalancePkr: 1000, updatedAt: now }));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("walletTransactions").doc("tx_fake").set({
      userId: ALICE_UID,
      type: "deposit",
      direction: "credit",
      amountPkr: 1000,
      title: "Fake Deposit",
      subtitle: "Hack",
      referenceId: "dep_1",
      status: "completed",
      balanceAfterPkr: 1000,
      createdAt: now,
    })
  );

  // Player submits valid EasyPaisa deposit request (status must be 'pending')
  const depPayload = {
    userId: ALICE_UID,
    inGameName: "PK_Shadow",
    userEmail: "alice@gmail.com",
    amountPkr: 500,
    paymentMethod: "easypaisa",
    receiverAccountNumber: "03489674834",
    transactionId: "EP99887766",
    senderNumber: "03451112233",
    screenshotBase64: "AAAA_VALID_BASE64_SCREENSHOT_PAYLOAD_DATA",
    status: "pending",
    adminNote: "",
    reviewedBy: "",
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(aliceDb.collection("depositRequests").doc("dep_1").set(depPayload));

  // Player CANNOT self-approve own deposit request
  await assertFails(aliceDb.collection("depositRequests").doc("dep_1").update({ status: "approved", updatedAt: now }));

  // Player CANNOT submit an already-approved deposit
  await assertFails(aliceDb.collection("depositRequests").doc("dep_2").set({ ...depPayload, status: "approved" }));

  // Bob CANNOT read Alice's deposit requests
  await assertFails(bobDb.collection("depositRequests").where("userId", "==", ALICE_UID).get());

  // Alice CAN list her own deposit requests
  await assertSucceeds(aliceDb.collection("depositRequests").where("userId", "==", ALICE_UID).get());

  // Admin CAN list and approve Alice's deposit request + credit balance
  await assertSucceeds(adminDb.collection("depositRequests").orderBy("createdAt", "desc").get());
  const approveBatch = adminDb.batch();
  approveBatch.update(adminDb.collection("depositRequests").doc("dep_1"), {
    status: "approved",
    adminNote: "Verified EasyPaisa TID",
    reviewedBy: ADMIN_UID,
    updatedAt: now,
  });
  approveBatch.update(adminDb.collection("users").doc(ALICE_UID), {
    walletBalancePkr: 500,
    updatedAt: now,
  });
  approveBatch.set(adminDb.collection("users").doc(ALICE_UID).collection("walletTransactions").doc("tx_dep_1"), {
    userId: ALICE_UID,
    type: "deposit",
    direction: "credit",
    amountPkr: 500,
    title: "EasyPaisa Deposit Approved",
    subtitle: "TID: EP99887766",
    referenceId: "dep_1",
    status: "completed",
    balanceAfterPkr: 500,
    createdAt: now,
  });
  await assertSucceeds(approveBatch.commit());

  // Withdrawal WITHOUT atomic balance hold must fail
  const wdPayload = {
    userId: ALICE_UID,
    inGameName: "PK_Shadow",
    userEmail: "alice@gmail.com",
    amountPkr: 200,
    paymentMethod: "jazzcash",
    accountNumber: "03001234567",
    accountTitle: "Ali Khan",
    status: "pending",
    adminNote: "",
    reviewedBy: "",
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(aliceDb.collection("withdrawalRequests").doc("wd_bad").set(wdPayload));

  // Withdrawal WITH atomic balance hold (500 -> 300) succeeds
  const wdBatch = aliceDb.batch();
  wdBatch.set(aliceDb.collection("withdrawalRequests").doc("wd_1"), wdPayload);
  wdBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletBalancePkr: 300,
    updatedAt: now,
  });
  wdBatch.set(aliceDb.collection("users").doc(ALICE_UID).collection("walletTransactions").doc("tx_wd_1"), {
    userId: ALICE_UID,
    type: "withdrawal",
    direction: "debit",
    amountPkr: 200,
    title: "JazzCash Withdrawal Request",
    subtitle: "To: 03001234567",
    referenceId: "wd_1",
    status: "pending",
    balanceAfterPkr: 300,
    createdAt: now,
  });
  await assertSucceeds(wdBatch.commit());
});

import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js";
import {
  getAuth,
  getRedirectResult,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signInWithRedirect,
  signOut
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  getFirestore,
  increment,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-firestore.js";
import { GAMERZONE_CONFIG } from "./config.js";

const app = initializeApp(GAMERZONE_CONFIG.firebase);
const auth = getAuth(app);
const db = getFirestore(app, GAMERZONE_CONFIG.firestoreDatabaseId);
const googleProvider = new GoogleAuthProvider();

const MIN_DEPOSIT_PKR = 100;
const MIN_WITHDRAWAL_PKR = 200;

function makeTidKey(methodRaw, tid) {
  const normalized = String(tid || "")
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9_-]/g, "_");
  return `${methodRaw}_${normalized}`.slice(0, 120);
}

function formatTs(ts) {
  if (!ts) return "Just now";
  const d = typeof ts.toDate === "function" ? ts.toDate() : new Date(ts);
  return d.toLocaleString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

// Application State
const state = {
  user: null,
  profile: null,
  isAdmin: false,
  tournaments: [],
  myRegistrations: [],
  leaderboard: [],
  announcements: [],
  walletTransactions: [],
  myDeposits: [],
  myWithdrawals: [],
  adminDeposits: [],
  adminWithdrawals: [],
  adminTransactions: [],
  activeTab: "tournaments",
  modeFilter: "All",
  statusFilter: "all",
  walletTab: "transactions",
  txFilter: "all",
  lbMode: "players",
  adminTab: "wallet",
  adminWalletSubTab: "deposits", // deposits | withdrawals | history
  adminWalletPendingOnly: true,
  adminWalletSearch: "",
  adminTxTypeFilter: "all",
  selectedResultsTournamentId: "",
  selectedTournamentRoster: [],
  unsubs: [],
  adminUnsubs: []
};

// DOM References
const el = {
  headerWalletPill: document.getElementById("headerWalletPill"),
  headerWalletBalance: document.getElementById("headerWalletBalance"),
  headerAuthBtn: document.getElementById("headerAuthBtn"),
  signedInUserMeta: document.getElementById("signedInUserMeta"),
  authGateCard: document.getElementById("authGateCard"),
  gateSignInBtn: document.getElementById("gateSignInBtn"),
  onboardingCard: document.getElementById("onboardingCard"),
  onboardingForm: document.getElementById("onboardingForm"),
  arenaWorkspace: document.getElementById("arenaWorkspace"),
  adminNavTab: document.getElementById("adminNavTab"),
  tournamentsList: document.getElementById("tournamentsList"),
  walletHeroBalance: document.getElementById("walletHeroBalance"),
  walletFeeFilterRow: document.getElementById("walletFeeFilterRow"),
  walletContent: document.getElementById("walletContent"),
  leaderboardList: document.getElementById("leaderboardList"),
  myMatchesList: document.getElementById("myMatchesList"),
  announcementsList: document.getElementById("announcementsList"),
  adminContent: document.getElementById("adminContent"),
  modalBackdrop: document.getElementById("modalBackdrop"),
  modalBox: document.getElementById("modalBox"),
  toast: document.getElementById("toast")
};

function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

let toastTimer = null;
function showToast(msg, isError = false) {
  el.toast.textContent = msg;
  el.toast.style.borderColor = isError ? "var(--crimson-live)" : "var(--neon-green)";
  el.toast.classList.remove("hidden");
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.toast.classList.add("hidden"), 4000);
}

function openModal(html) {
  el.modalBox.innerHTML = html;
  el.modalBackdrop.classList.remove("hidden");
}

function closeModal() {
  el.modalBackdrop.classList.add("hidden");
  el.modalBox.innerHTML = "";
}

el.modalBackdrop.addEventListener("click", (e) => {
  if (e.target === el.modalBackdrop) closeModal();
});

async function copyText(text, label = "Copied") {
  try {
    await navigator.clipboard.writeText(text);
    showToast(`${label}: ${text}`);
  } catch {
    showToast(text);
  }
}

document.querySelectorAll(".copy-btn").forEach((btn) => {
  btn.addEventListener("click", () => copyText(btn.dataset.copy, "Account Number Copied"));
});

function syncApkDownloadMetadata() {
  const downloadIds = ["headerDownloadBtn", "heroDownloadBtn", "mobileDownloadBtn"];
  downloadIds.forEach((id) => {
    const link = document.getElementById(id);
    if (link) {
      link.setAttribute("href", GAMERZONE_CONFIG.apkDownloadUrl);
      link.setAttribute("download", GAMERZONE_CONFIG.apkFileName);
    }
  });
  const setText = (id, text) => {
    const node = document.getElementById(id);
    if (node) node.textContent = text;
  };
  setText("metaVersion", GAMERZONE_CONFIG.apkVersion);
  setText("metaSize", GAMERZONE_CONFIG.apkSizeLabel);
  setText("metaAndroid", GAMERZONE_CONFIG.minAndroid);
  setText("mobileApkTitle", `GamerZone PK APK (${GAMERZONE_CONFIG.apkVersion})`);
  setText("mobileApkMeta", `Direct Free Download • ${GAMERZONE_CONFIG.apkSizeLabel}`);
  const heroBtn = document.getElementById("heroDownloadBtn");
  if (heroBtn) {
    heroBtn.textContent = `⬇ Download Android APK (${GAMERZONE_CONFIG.apkVersion})`;
  }
}
syncApkDownloadMetadata();

getRedirectResult(auth).catch((err) => {
  if (err?.code === "auth/unauthorized-domain") {
    showToast(
      `Add ${window.location.hostname} under Firebase Console -> Authentication -> Settings -> Authorized domains.`,
      true
    );
  } else if (err?.message) {
    showToast(err.message, true);
  }
});

// APK Install Modal
document.getElementById("heroInstallHelpBtn")?.addEventListener("click", () => {
  openModal(`
    <div class="row-between" style="margin-bottom:12px;">
      <h3>How to Install GamerZone PK APK</h3>
      <button class="btn btn-ghost btn-sm" id="closeModalBtn" type="button">Close</button>
    </div>
    <ol style="padding-left:18px; display:grid; gap:10px; color:var(--text-secondary);">
      <li>Tap <strong>Download Android APK</strong> to save <code>${escapeHtml(GAMERZONE_CONFIG.apkFileName)}</code> (${escapeHtml(GAMERZONE_CONFIG.apkSizeLabel)}).</li>
      <li>Open your phone's <strong>Downloads</strong> folder or browser notification and tap the APK file.</li>
      <li>If Android asks for permission, tap <strong>Settings</strong> and toggle <strong>Allow from this source</strong>.</li>
      <li>Tap <strong>Install</strong>, open <strong>GamerZone PK</strong>, and sign in with your Google account.</li>
    </ol>
    <div style="margin-top:16px; display:flex; gap:10px; flex-wrap:wrap;">
      <a class="btn btn-primary" href="${escapeHtml(GAMERZONE_CONFIG.apkDownloadUrl)}" download="${escapeHtml(GAMERZONE_CONFIG.apkFileName)}">⬇ Download APK Now</a>
    </div>
  `);
  document.getElementById("closeModalBtn")?.addEventListener("click", closeModal);
});

// Auth Handling
async function handleSignIn() {
  try {
    await signInWithPopup(auth, googleProvider);
  } catch (err) {
    if (err?.code === "auth/popup-blocked") {
      await signInWithRedirect(auth, googleProvider);
    } else if (err?.code === "auth/unauthorized-domain") {
      showToast(
        `Add ${window.location.hostname} under Firebase Console -> Authentication -> Settings -> Authorized domains.`,
        true
      );
    } else {
      showToast(err?.message || "Google Sign-In failed.", true);
    }
  }
}

el.headerAuthBtn.addEventListener("click", () => {
  if (state.user) {
    switchTab("player");
    document.getElementById("arena")?.scrollIntoView({ behavior: "smooth" });
  } else {
    handleSignIn();
  }
});

el.gateSignInBtn.addEventListener("click", handleSignIn);

el.headerWalletPill.addEventListener("click", () => {
  switchTab("wallet");
  document.getElementById("arena")?.scrollIntoView({ behavior: "smooth" });
});

document.getElementById("signOutBtn")?.addEventListener("click", async () => {
  await signOut(auth);
  showToast("Signed out.");
});

function clearSubscriptions() {
  state.unsubs.forEach((u) => u());
  state.unsubs = [];
  clearAdminSubscriptions();
}

function clearAdminSubscriptions() {
  state.adminUnsubs.forEach((u) => u());
  state.adminUnsubs = [];
}

onAuthStateChanged(auth, (user) => {
  clearSubscriptions();
  state.user = user;
  state.profile = null;
  state.isAdmin = false;

  if (!user) {
    el.headerAuthBtn.textContent = "Sign In with Google";
    el.headerWalletPill.classList.add("hidden");
    el.signedInUserMeta.classList.add("hidden");
    el.authGateCard.classList.remove("hidden");
    el.onboardingCard.classList.add("hidden");
    el.arenaWorkspace.classList.add("hidden");
    return;
  }

  el.headerAuthBtn.textContent = user.displayName ? user.displayName.split(" ")[0] : "Account";
  el.authGateCard.classList.add("hidden");
  startUserStreams(user);
});

function startUserStreams(user) {
  // 1. User Profile
  state.unsubs.push(
    onSnapshot(
      doc(db, "users", user.uid),
      (snap) => {
        state.profile = snap.exists() ? { id: snap.id, ...snap.data() } : null;
        renderAuthState();
        renderWallet();
        renderPlayerDashboard();
      },
      (err) => showToast(err.message, true)
    )
  );

  // 2. Admin status
  const isBootstrapped = user.email === GAMERZONE_CONFIG.bootstrappedAdminEmail && user.emailVerified;
  if (isBootstrapped) {
    setAdminMode(true);
  } else {
    state.unsubs.push(
      onSnapshot(
        doc(db, "admins", user.uid),
        (snap) => setAdminMode(snap.exists()),
        () => setAdminMode(false)
      )
    );
  }

  // 3. Tournaments
  state.unsubs.push(
    onSnapshot(
      query(collection(db, "tournaments"), orderBy("createdAt", "desc")),
      (snap) => {
        state.tournaments = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        if (!state.selectedResultsTournamentId && state.tournaments.length > 0) {
          state.selectedResultsTournamentId = state.tournaments[0].id;
        }
        renderTournaments();
        if (state.isAdmin && state.activeTab === "admin") renderAdmin();
      },
      (err) => showToast(err.message, true)
    )
  );

  // 4. My Registrations
  state.unsubs.push(
    onSnapshot(
      collection(db, "users", user.uid, "registrations"),
      (snap) => {
        state.myRegistrations = snap.docs
          .map((d) => ({ id: d.id, ...d.data() }))
          .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
        renderTournaments();
        renderPlayerDashboard();
      }
    )
  );

  // 5. Leaderboard
  state.unsubs.push(
    onSnapshot(
      query(collection(db, "leaderboard"), orderBy("totalPoints", "desc")),
      (snap) => {
        state.leaderboard = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderLeaderboard();
      }
    )
  );

  // 6. Announcements
  state.unsubs.push(
    onSnapshot(
      query(collection(db, "announcements"), orderBy("createdAt", "desc")),
      (snap) => {
        state.announcements = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderPlayerDashboard();
        if (state.isAdmin && state.activeTab === "admin") renderAdmin();
      }
    )
  );

  // 7. Wallet Transactions
  state.unsubs.push(
    onSnapshot(
      query(collection(db, "users", user.uid, "walletTransactions"), orderBy("createdAt", "desc")),
      (snap) => {
        state.walletTransactions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderWallet();
      }
    )
  );

  // 8. My Deposit Requests
  state.unsubs.push(
    onSnapshot(
      query(collection(db, "depositRequests"), where("userId", "==", user.uid)),
      (snap) => {
        state.myDeposits = snap.docs
          .map((d) => ({ id: d.id, ...d.data() }))
          .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
        renderWallet();
      }
    )
  );

  // 9. My Withdrawal Requests
  state.unsubs.push(
    onSnapshot(
      query(collection(db, "withdrawalRequests"), where("userId", "==", user.uid)),
      (snap) => {
        state.myWithdrawals = snap.docs
          .map((d) => ({ id: d.id, ...d.data() }))
          .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
        renderWallet();
      }
    )
  );
}

function setAdminMode(enabled) {
  state.isAdmin = enabled;
  clearAdminSubscriptions();
  el.adminNavTab.classList.toggle("hidden", !enabled);

  if (enabled) {
    state.adminUnsubs.push(
      onSnapshot(
        query(collection(db, "depositRequests"), orderBy("createdAt", "desc")),
        (snap) => {
          state.adminDeposits = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
          if (state.activeTab === "admin") renderAdmin();
        }
      )
    );
    state.adminUnsubs.push(
      onSnapshot(
        query(collection(db, "withdrawalRequests"), orderBy("createdAt", "desc")),
        (snap) => {
          state.adminWithdrawals = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
          if (state.activeTab === "admin") renderAdmin();
        }
      )
    );
    state.adminUnsubs.push(
      onSnapshot(
        query(collection(db, "adminTransactions"), orderBy("createdAt", "desc")),
        (snap) => {
          state.adminTransactions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
          if (state.activeTab === "admin") renderAdmin();
        }
      )
    );
  }
  renderAuthState();
}

function renderAuthState() {
  if (!state.user) return;
  if (!state.profile) {
    el.onboardingCard.classList.remove("hidden");
    el.arenaWorkspace.classList.add("hidden");
    el.headerWalletPill.classList.add("hidden");
    return;
  }

  el.onboardingCard.classList.add("hidden");
  el.arenaWorkspace.classList.remove("hidden");
  el.headerWalletPill.classList.remove("hidden");
  const balance = Number(state.profile.walletBalancePkr || 0);
  el.headerWalletBalance.textContent = `${balance} PKR`;
  el.walletHeroBalance.textContent = `${balance} PKR`;

  el.signedInUserMeta.classList.remove("hidden");
  el.signedInUserMeta.textContent = `${state.profile.inGameName} • ${state.isAdmin ? "ADMIN" : "PLAYER"}`;
}

// Onboarding Form
el.onboardingForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!state.user) return;
  const ign = document.getElementById("onbIgn").value.trim();
  const ffUid = document.getElementById("onbUid").value.trim();
  const team = document.getElementById("onbTeam").value.trim() || "Solo Wolf";
  const wa = document.getElementById("onbWa").value.trim();
  const displayName = (state.user.displayName || ign).trim().slice(0, 60);
  const email = (state.user.email || `${state.user.uid}@player.gamerzonepk`).trim().slice(0, 200);

  try {
    const batch = writeBatch(db);
    batch.set(doc(db, "users", state.user.uid), {
      uid: state.user.uid,
      displayName,
      email,
      inGameName: ign,
      freeFireUid: ffUid,
      whatsappNumber: wa,
      teamName: team,
      role: "player",
      matchesPlayed: 0,
      totalWins: 0,
      matchPoints: 0,
      killPoints: 0,
      totalPoints: 0,
      walletBalancePkr: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    batch.set(doc(db, "leaderboard", state.user.uid), {
      userId: state.user.uid,
      inGameName: ign,
      freeFireUid: ffUid,
      teamName: team,
      matchesPlayed: 0,
      totalWins: 0,
      matchPoints: 0,
      killPoints: 0,
      totalPoints: 0,
      updatedAt: serverTimestamp()
    });
    await batch.commit();
    showToast("Player profile created!");
  } catch (err) {
    showToast(err.message || "Failed to save profile.", true);
  }
});

// Tab Switching
function switchTab(tabName) {
  state.activeTab = tabName;
  document.querySelectorAll(".tab-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.tab === tabName);
  });
  document.querySelectorAll(".tab-panel").forEach((p) => {
    p.classList.toggle("hidden", p.id !== `tab-${tabName}`);
  });
  if (tabName === "admin") renderAdmin();
}

document.querySelectorAll(".tab-btn").forEach((btn) => {
  btn.addEventListener("click", () => switchTab(btn.dataset.tab));
});

// Filters
document.querySelectorAll("#modeFilters .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    state.modeFilter = chip.dataset.mode;
    document.querySelectorAll("#modeFilters .chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderTournaments();
  });
});

document.querySelectorAll("#statusFilters .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    state.statusFilter = chip.dataset.status;
    document.querySelectorAll("#statusFilters .chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderTournaments();
  });
});

// Tournaments Renderer
function renderTournaments() {
  if (!state.profile) return;
  const joinedSet = new Set(state.myRegistrations.map((r) => r.tournamentId));
  const filtered = state.tournaments.filter((t) => {
    const modeOk = state.modeFilter === "All" || t.mode === state.modeFilter;
    const statusOk = state.statusFilter === "all" || t.status === state.statusFilter;
    return modeOk && statusOk;
  });

  if (filtered.length === 0) {
    el.tournamentsList.innerHTML = `<div class="card"><p class="section-sub">No tournaments match the selected filters yet.</p></div>`;
    return;
  }

  el.tournamentsList.innerHTML = filtered
    .map((t) => {
      const joined = joinedSet.has(t.id);
      const total = Math.max(1, Number(t.totalSlots || 48));
      const regCount = Number(t.registeredCount || 0);
      const pct = Math.min(100, Math.round((regCount / total) * 100));
      const statusBadge =
        t.status === "live"
          ? `<span class="badge badge-crimson">LIVE</span>`
          : t.status === "completed"
          ? `<span class="badge badge-gold">COMPLETED</span>`
          : `<span class="badge badge-green">UPCOMING</span>`;

      const showRoom = (joined || state.isAdmin) && (t.roomId || t.roomPassword);

      return `
        <div class="card card-neon">
          <div class="row-between">
            <div style="display:flex; gap:6px; flex-wrap:wrap;">
              <span class="badge badge-cyan">${escapeHtml(t.mode)}</span>
              ${statusBadge}
              ${joined ? `<span class="badge badge-green">JOINED</span>` : ""}
            </div>
            <strong style="color:var(--gold-trophy);">${Number(t.prizePoolPkr || 0)} PKR Pool</strong>
          </div>
          <h3 style="margin-top:10px; font-size:1.2rem;">${escapeHtml(t.title)}</h3>
          <p class="section-sub">${escapeHtml(t.mapName)} • ${escapeHtml(t.matchTime)}</p>

          <div class="slot-track"><div class="slot-fill" style="width:${pct}%"></div></div>
          <div class="row-between" style="font-size:0.88rem; color:var(--text-secondary);">
            <span>Slots: <strong>${regCount}/${total}</strong></span>
            <span>Entry Fee: <strong style="color:var(--neon-green);">${Number(t.entryFeePkr || 0)} PKR</strong></span>
          </div>
          <div style="margin-top:8px; font-size:0.84rem; color:var(--text-secondary);">
            1st: ${Number(t.firstPrizePkr || 0)} PKR • 2nd: ${Number(t.secondPrizePkr || 0)} PKR • 3rd: ${Number(t.thirdPrizePkr || 0)} PKR
          </div>
          ${t.winnerName ? `<div style="margin-top:8px; color:var(--gold-trophy); font-weight:700;">🏆 Winner: ${escapeHtml(t.winnerName)}</div>` : ""}
          ${
            showRoom
              ? `<div class="card" style="margin-top:10px; padding:10px; background:var(--bg-surface);">
                  <div style="font-size:0.8rem; color:var(--cyan-accent); font-weight:700;">CUSTOM ROOM CREDENTIALS</div>
                  <div class="mono" style="margin-top:4px;">ID: ${escapeHtml(t.roomId || "TBA")} | PASS: ${escapeHtml(t.roomPassword || "TBA")}</div>
                </div>`
              : ""
          }
          <div style="margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;">
            ${
              joined
                ? `<button class="btn btn-ghost btn-sm" disabled>Slot Booked</button>`
                : t.status === "upcoming" && regCount < total
                ? `<button class="btn btn-primary btn-sm join-tourney-btn" data-tid="${escapeHtml(t.id)}" type="button">Join (${Number(t.entryFeePkr || 0)} PKR)</button>`
                : `<button class="btn btn-ghost btn-sm" disabled>${regCount >= total ? "Full" : "Closed"}</button>`
            }
          </div>
        </div>
      `;
    })
    .join("");

  document.querySelectorAll(".join-tourney-btn").forEach((btn) => {
    btn.addEventListener("click", () => openJoinConfirmModal(btn.dataset.tid));
  });
}

function openJoinConfirmModal(tid) {
  const t = state.tournaments.find((x) => x.id === tid);
  if (!t || !state.profile) return;
  const fee = Number(t.entryFeePkr || 0);
  const bal = Number(state.profile.walletBalancePkr || 0);
  const enough = bal >= fee;

  openModal(`
    <h3 style="margin-bottom:8px;">Confirm Tournament Entry</h3>
    <p class="section-sub">${escapeHtml(t.title)} (${escapeHtml(t.mode)} • ${escapeHtml(t.mapName)})</p>
    <div class="card" style="margin:12px 0; background:var(--bg-surface);">
      <div class="row-between"><span>Your Wallet Balance:</span><strong>${bal} PKR</strong></div>
      <div class="row-between" style="margin-top:6px;"><span>Tournament Entry Fee:</span><strong style="color:var(--gold-trophy);">${fee} PKR</strong></div>
      <div class="row-between" style="margin-top:6px;"><span>Balance After Joining:</span><strong style="color:${enough ? "var(--neon-green)" : "var(--crimson-live)"};">${bal - fee} PKR</strong></div>
    </div>
    ${
      enough
        ? `<div style="display:flex; gap:10px; justify-content:flex-end;">
             <button class="btn btn-ghost" id="cancelJoinBtn" type="button">Cancel</button>
             <button class="btn btn-primary" id="confirmJoinBtn" type="button">Confirm & Join</button>
           </div>`
        : `<p style="color:var(--crimson-live); font-weight:700; margin-bottom:12px;">Insufficient wallet balance. Deposit via EasyPaisa or JazzCash first.</p>
           <div style="display:flex; gap:10px; justify-content:flex-end;">
             <button class="btn btn-ghost" id="cancelJoinBtn" type="button">Close</button>
             <button class="btn btn-primary" id="goAddMoneyBtn" type="button">+ Add Money</button>
           </div>`
    }
  `);

  document.getElementById("cancelJoinBtn")?.addEventListener("click", closeModal);
  document.getElementById("goAddMoneyBtn")?.addEventListener("click", () => {
    closeModal();
    switchTab("wallet");
    openAddMoneyModal();
  });
  document.getElementById("confirmJoinBtn")?.addEventListener("click", async () => {
    await executeJoinTournament(t);
    closeModal();
  });
}

async function executeJoinTournament(tournament) {
  if (!state.user || !state.profile) return;
  const uid = state.user.uid;
  const tournamentRef = doc(db, "tournaments", tournament.id);
  const tourneyRegRef = doc(db, "tournaments", tournament.id, "registrations", uid);
  const userRef = doc(db, "users", uid);
  const userRegRef = doc(db, "users", uid, "registrations", tournament.id);
  const txDocRef = doc(collection(db, "users", uid, "walletTransactions"));

  try {
    await runTransaction(db, async (tx) => {
      const tourneySnap = await tx.get(tournamentRef);
      if (!tourneySnap.exists()) throw new Error("Tournament no longer exists.");
      const regSnap = await tx.get(tourneyRegRef);
      if (regSnap.exists()) throw new Error("You are already registered.");
      const userSnap = await tx.get(userRef);
      if (!userSnap.exists()) throw new Error("Profile not found.");

      const tData = tourneySnap.data();
      if (tData.status !== "upcoming") throw new Error("Only upcoming tournaments can be joined.");
      if ((tData.registeredCount || 0) >= (tData.totalSlots || 0)) throw new Error("All slots are full.");

      const entryFee = Number(tData.entryFeePkr || 0);
      const currentBal = Number(userSnap.data().walletBalancePkr || 0);
      if (currentBal < entryFee) throw new Error(`Insufficient balance (${currentBal} PKR).`);

      const slotNumber = Math.min(tData.totalSlots, (tData.registeredCount || 0) + 1);
      const regPayload = {
        tournamentId: tournament.id,
        tournamentTitle: tData.title,
        tournamentMode: tData.mode,
        tournamentStatus: tData.status,
        matchTime: tData.matchTime,
        mapName: tData.mapName,
        entryFeePkr: entryFee,
        prizePoolPkr: Number(tData.prizePoolPkr || 0),
        userId: uid,
        inGameName: state.profile.inGameName,
        freeFireUid: state.profile.freeFireUid,
        teamName: state.profile.teamName,
        slotNumber,
        matchPoints: 0,
        killPoints: 0,
        totalPoints: 0,
        placement: 0,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      };

      tx.set(tourneyRegRef, regPayload);
      tx.set(userRegRef, regPayload);
      tx.update(tournamentRef, {
        registeredCount: (tData.registeredCount || 0) + 1,
        updatedAt: serverTimestamp()
      });

      if (entryFee > 0) {
        const newBal = currentBal - entryFee;
        tx.update(userRef, {
          walletBalancePkr: newBal,
          updatedAt: serverTimestamp()
        });
        const txPayload = {
          userId: uid,
          type: "tournament_entry",
          direction: "debit",
          amountPkr: entryFee,
          title: `Joined: ${tData.title}`.slice(0, 100),
          subtitle: `${tData.mode} • ${tData.mapName} • Slot #${slotNumber}`.slice(0, 150),
          referenceId: tournament.id,
          status: "completed",
          balanceAfterPkr: newBal,
          createdAt: serverTimestamp()
        };
        tx.set(txDocRef, txPayload);
        tx.set(doc(db, "adminTransactions", txDocRef.id), {
          ...txPayload,
          inGameName: state.profile.inGameName,
          userEmail: state.profile.email
        });
      }
    });
    showToast("Slot booked! Entry fee deducted from wallet.");
  } catch (err) {
    showToast(err.message || "Unable to join tournament.", true);
  }
}

// Wallet Sub-Tabs & Rendering
document.querySelectorAll("#walletSubTabs .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    state.walletTab = chip.dataset.wtab;
    document.querySelectorAll("#walletSubTabs .chip").forEach((c) => c.classList.toggle("active", c === chip));
    el.walletFeeFilterRow.classList.toggle("hidden", state.walletTab !== "transactions");
    renderWallet();
  });
});

document.querySelectorAll("#walletFeeFilterRow .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    state.txFilter = chip.dataset.txfilter;
    document.querySelectorAll("#walletFeeFilterRow .chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderWallet();
  });
});

function renderStatusBadge(status) {
  const s = String(status || "pending").toLowerCase();
  const label = s.charAt(0).toUpperCase() + s.slice(1);
  if (s === "approved" || s === "completed") return `<span class="badge badge-green">${escapeHtml(label)}</span>`;
  if (s === "rejected") return `<span class="badge badge-crimson">${escapeHtml(label)}</span>`;
  return `<span class="badge badge-gold">${escapeHtml(label)}</span>`;
}

function renderWallet() {
  if (!state.profile) return;
  const bal = Number(state.profile.walletBalancePkr || 0);
  el.walletHeroBalance.textContent = `${bal} PKR`;
  el.headerWalletBalance.textContent = `${bal} PKR`;

  if (state.walletTab === "transactions") {
    const items =
      state.txFilter === "tournament_entry"
        ? state.walletTransactions.filter((t) => t.type === "tournament_entry")
        : state.walletTransactions;
    if (items.length === 0) {
      el.walletContent.innerHTML = `<div class="card"><p class="section-sub">No wallet transactions yet.</p></div>`;
      return;
    }
    el.walletContent.innerHTML = items
      .map((tx) => {
        const isCredit = tx.direction === "credit";
        return `
          <div class="card">
            <div class="row-between">
              <div>
                <strong>${escapeHtml(tx.title)}</strong>
                <div class="section-sub">${escapeHtml(tx.subtitle)}</div>
              </div>
              <div style="text-align:right;">
                <div style="font-weight:700; font-size:1.1rem; color:${isCredit ? "var(--neon-green)" : "var(--crimson-live)"};">
                  ${isCredit ? "+" : "-"}${Number(tx.amountPkr || 0)} PKR
                </div>
                <div class="section-sub">Bal: ${Number(tx.balanceAfterPkr || 0)} PKR • ${escapeHtml(tx.status)}</div>
              </div>
            </div>
          </div>
        `;
      })
      .join("");
  } else if (state.walletTab === "deposits") {
    if (state.myDeposits.length === 0) {
      el.walletContent.innerHTML = `<div class="card"><p class="section-sub">No deposit requests submitted yet.</p></div>`;
      return;
    }
    el.walletContent.innerHTML = state.myDeposits
      .map(
        (d) => `
      <div class="card">
        <div class="row-between">
          <div>
            <strong>${escapeHtml(d.paymentMethod?.toUpperCase())} • ${Number(d.amountPkr || 0)} PKR</strong>
            <div class="section-sub">TID: <span class="mono">${escapeHtml(d.transactionId)}</span> • To: ${escapeHtml(d.receiverAccountNumber)}</div>
            ${d.adminNote ? `<div class="section-sub">Admin Note: ${escapeHtml(d.adminNote)}</div>` : ""}
          </div>
          <div style="display:flex; gap:8px; align-items:center;">
            ${renderStatusBadge(d.status)}
            ${
              d.screenshotBase64
                ? `<button class="btn btn-ghost btn-sm view-shot-btn" data-depid="${escapeHtml(d.id)}" type="button">View Proof</button>`
                : ""
            }
          </div>
        </div>
      </div>`
      )
      .join("");
    bindScreenshotButtons();
  } else if (state.walletTab === "withdrawals") {
    if (state.myWithdrawals.length === 0) {
      el.walletContent.innerHTML = `<div class="card"><p class="section-sub">No withdrawal requests submitted yet.</p></div>`;
      return;
    }
    el.walletContent.innerHTML = state.myWithdrawals
      .map(
        (w) => `
      <div class="card">
        <div class="row-between">
          <div>
            <strong>${escapeHtml(w.paymentMethod?.toUpperCase())} • ${Number(w.amountPkr || 0)} PKR</strong>
            <div class="section-sub">To: ${escapeHtml(w.accountNumber)} (${escapeHtml(w.accountTitle)})</div>
            ${w.adminNote ? `<div class="section-sub">Admin Note: ${escapeHtml(w.adminNote)}</div>` : ""}
          </div>
          ${renderStatusBadge(w.status)}
        </div>
      </div>`
      )
      .join("");
  } else {
    el.walletContent.innerHTML = `
      <div class="grid grid-2">
        <div class="card card-neon">
          <div class="badge badge-green">EasyPaisa</div>
          <h3 class="mono" style="margin-top:8px; color:var(--neon-green);">03489674834</h3>
          <p class="section-sub" style="margin:6px 0 12px;">Send PKR deposits to this EasyPaisa account and submit your TID & screenshot.</p>
          <button class="btn btn-outline btn-sm copy-inline-btn" data-copy="03489674834" type="button">Copy EasyPaisa</button>
        </div>
        <div class="card card-cyan">
          <div class="badge badge-cyan">JazzCash</div>
          <h3 class="mono" style="margin-top:8px; color:var(--cyan-accent);">03406690336</h3>
          <p class="section-sub" style="margin:6px 0 12px;">Send PKR deposits to this JazzCash account and submit your TID & screenshot.</p>
          <button class="btn btn-cyan btn-sm copy-inline-btn" data-copy="03406690336" type="button">Copy JazzCash</button>
        </div>
      </div>
    `;
    el.walletContent.querySelectorAll(".copy-inline-btn").forEach((b) => {
      b.addEventListener("click", () => copyText(b.dataset.copy, "Account Copied"));
    });
  }
}

function bindScreenshotButtons() {
  document.querySelectorAll(".view-shot-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const depId = btn.dataset.depid || "";
      const record =
        state.adminDeposits.find((d) => d.id === depId) ||
        state.myDeposits.find((d) => d.id === depId);
      const raw = record?.screenshotBase64 || btn.dataset.shot || "";
      if (!raw) {
        showToast("Screenshot not available.", true);
        return;
      }
      const src = raw.startsWith("data:image/") ? raw : `data:image/jpeg;base64,${raw}`;
      openModal(`
        <div class="row-between" style="margin-bottom:12px;">
          <h3>Payment Screenshot Proof</h3>
          <button class="btn btn-ghost btn-sm" id="closeShotBtn" type="button">Close</button>
        </div>
        <img src="${escapeHtml(src)}" alt="Payment Screenshot" style="width:100%; max-height:70vh; object-fit:contain; border-radius:10px;" />
      `);
      document.getElementById("closeShotBtn")?.addEventListener("click", closeModal);
    });
  });
}

// Client-side Canvas JPEG compression matching Android ScreenshotUtils
function compressImageToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const maxSide = 720;
        const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
        const w = Math.max(1, Math.round(img.width * scale));
        const h = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, w, h);

        let quality = 0.75;
        let dataUrl = canvas.toDataURL("image/jpeg", quality);
        while (dataUrl.length > 260000 && quality > 0.3) {
          quality -= 0.1;
          dataUrl = canvas.toDataURL("image/jpeg", quality);
        }
        const base64 = dataUrl.split(",")[1] || "";
        if (base64.length < 20 || base64.length > 580000) {
          reject(new Error("Screenshot is too large. Choose a smaller image."));
        } else {
          resolve({ base64, previewUrl: dataUrl });
        }
      };
      img.onerror = () => reject(new Error("Could not decode image."));
      img.src = reader.result;
    };
    reader.onerror = () => reject(new Error("Failed to read file."));
    reader.readAsDataURL(file);
  });
}

document.getElementById("openAddMoneyBtn")?.addEventListener("click", openAddMoneyModal);
document.getElementById("openWithdrawBtn")?.addEventListener("click", openWithdrawModal);

function openAddMoneyModal() {
  if (!state.profile) return;
  let uploadedBase64 = "";

  openModal(`
    <div class="row-between" style="margin-bottom:12px;">
      <h3>Add Money to Wallet</h3>
      <button class="btn btn-ghost btn-sm" id="closeDepModal" type="button">Close</button>
    </div>
    <form id="addMoneyForm">
      <div class="field">
        <label for="depMethod">Select Payment Method</label>
        <select id="depMethod" class="select">
          <option value="easypaisa">EasyPaisa — 03489674834</option>
          <option value="jazzcash">JazzCash — 03406690336</option>
        </select>
      </div>
      <div class="card" style="background:var(--bg-surface); margin-bottom:12px;">
        <div class="row-between">
          <div>
            <div class="section-sub">Send PKR to this Account Number:</div>
            <div id="depAccountDisplay" class="mono" style="font-size:1.25rem; font-weight:700; color:var(--neon-green);">03489674834</div>
          </div>
          <button class="btn btn-outline btn-sm" id="copyDepAccBtn" type="button">Copy</button>
        </div>
      </div>
      <div class="field">
        <label for="depAmount">Deposit Amount (Min ${MIN_DEPOSIT_PKR} PKR) *</label>
        <input id="depAmount" class="input" type="number" min="${MIN_DEPOSIT_PKR}" max="500000" required placeholder="Min ${MIN_DEPOSIT_PKR} PKR (e.g. 200)" />
        <small class="section-sub">Minimum deposit: ${MIN_DEPOSIT_PKR} PKR. Balance is added only after Admin verification.</small>
      </div>
      <div class="field">
        <label for="depTid">Transaction ID (TID) *</label>
        <input id="depTid" class="input" minlength="4" maxlength="64" required placeholder="Enter EasyPaisa / JazzCash TID" />
      </div>
      <div class="field">
        <label for="depSender">Sender Mobile Number (Optional)</label>
        <input id="depSender" class="input" maxlength="25" placeholder="03XX-XXXXXXX" />
      </div>
      <div class="field">
        <label for="depShot">Upload Payment Screenshot *</label>
        <input id="depShot" class="input" type="file" accept="image/*" required />
      </div>
      <img id="depPreview" class="screenshot-thumb hidden" alt="Screenshot Preview" style="margin-bottom:12px;" />
      <button id="submitDepBtn" class="btn btn-primary" style="width:100%;" type="submit">Submit Deposit for Admin Approval</button>
    </form>
  `);

  const methodSel = document.getElementById("depMethod");
  const accDisp = document.getElementById("depAccountDisplay");
  methodSel.addEventListener("change", () => {
    const num = GAMERZONE_CONFIG.paymentMethods[methodSel.value].accountNumber;
    accDisp.textContent = num;
  });

  document.getElementById("copyDepAccBtn").addEventListener("click", () => {
    copyText(accDisp.textContent, "Payment Account Copied");
  });
  document.getElementById("closeDepModal").addEventListener("click", closeModal);

  document.getElementById("depShot").addEventListener("change", async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await compressImageToBase64(file);
      uploadedBase64 = res.base64;
      const preview = document.getElementById("depPreview");
      preview.src = res.previewUrl;
      preview.classList.remove("hidden");
    } catch (err) {
      showToast(err.message, true);
    }
  });

  document.getElementById("addMoneyForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!uploadedBase64) {
      showToast("Please upload a payment screenshot.", true);
      return;
    }
    const methodKey = methodSel.value;
    const methodObj = GAMERZONE_CONFIG.paymentMethods[methodKey];
    const amountPkr = parseInt(document.getElementById("depAmount").value, 10);
    const transactionId = document.getElementById("depTid").value.trim();
    const senderNumber = document.getElementById("depSender").value.trim();

    if (!Number.isFinite(amountPkr) || amountPkr < MIN_DEPOSIT_PKR || amountPkr > 500000) {
      showToast(`Minimum deposit amount is ${MIN_DEPOSIT_PKR} PKR.`, true);
      return;
    }
    if (transactionId.length < 4) {
      showToast("Enter a valid Transaction ID (min 4 characters).", true);
      return;
    }

    const tidKey = makeTidKey(methodObj.raw, transactionId);

    try {
      await runTransaction(db, async (tx) => {
        const lockRef = doc(db, "usedTransactionIds", tidKey);
        const lockSnap = await tx.get(lockRef);
        if (lockSnap.exists()) {
          throw new Error("This Transaction ID has already been submitted. Duplicate deposits are blocked.");
        }
        const depRef = doc(collection(db, "depositRequests"));
        tx.set(lockRef, {
          depositId: depRef.id,
          userId: state.user.uid,
          paymentMethod: methodObj.raw,
          transactionId,
          createdAt: serverTimestamp()
        });
        tx.set(depRef, {
          userId: state.user.uid,
          inGameName: state.profile.inGameName,
          userEmail: state.profile.email,
          amountPkr,
          paymentMethod: methodObj.raw,
          receiverAccountNumber: methodObj.accountNumber,
          transactionId,
          tidKey,
          senderNumber,
          screenshotBase64: uploadedBase64,
          status: "pending",
          adminNote: "",
          reviewedBy: "",
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
      });
      closeModal();
      showToast("Deposit submitted! Balance will be credited after Admin verification.");
    } catch (err) {
      showToast(err.message || "Deposit request failed.", true);
    }
  });
}

function openWithdrawModal() {
  if (!state.profile) return;
  const bal = Number(state.profile.walletBalancePkr || 0);
  openModal(`
    <div class="row-between" style="margin-bottom:12px;">
      <h3>Withdraw Money (Available: ${bal} PKR)</h3>
      <button class="btn btn-ghost btn-sm" id="closeWdModal" type="button">Close</button>
    </div>
    <form id="withdrawForm">
      <div class="field">
        <label for="wdMethod">Payout Method</label>
        <select id="wdMethod" class="select">
          <option value="easypaisa">EasyPaisa</option>
          <option value="jazzcash">JazzCash</option>
        </select>
      </div>
      <div class="field">
        <label for="wdAmount">Amount (Min ${MIN_WITHDRAWAL_PKR} PKR) *</label>
        <input id="wdAmount" class="input" type="number" min="${MIN_WITHDRAWAL_PKR}" max="${Math.max(MIN_WITHDRAWAL_PKR, bal)}" required placeholder="Min ${MIN_WITHDRAWAL_PKR} PKR (e.g. 500)" />
        <small class="section-sub">Minimum withdrawal: ${MIN_WITHDRAWAL_PKR} PKR. Status starts as Pending until Admin payout.</small>
      </div>
      <div class="field">
        <label for="wdAcc">Your EasyPaisa / JazzCash Number *</label>
        <input id="wdAcc" class="input" minlength="7" maxlength="20" required placeholder="03XXXXXXXXX" />
      </div>
      <div class="field">
        <label for="wdTitle">Account Holder Full Name *</label>
        <input id="wdTitle" class="input" minlength="2" maxlength="60" required placeholder="e.g. Ali Raza" />
      </div>
      <button class="btn btn-cyan" style="width:100%;" type="submit">Submit Withdrawal Request</button>
    </form>
  `);

  document.getElementById("closeWdModal").addEventListener("click", closeModal);
  document.getElementById("withdrawForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const methodKey = document.getElementById("wdMethod").value;
    const amountPkr = parseInt(document.getElementById("wdAmount").value, 10);
    const accountNumber = document.getElementById("wdAcc").value.trim();
    const accountTitle = document.getElementById("wdTitle").value.trim();

    if (!Number.isFinite(amountPkr) || amountPkr < MIN_WITHDRAWAL_PKR || amountPkr > 500000) {
      showToast(`Minimum withdrawal amount is ${MIN_WITHDRAWAL_PKR} PKR.`, true);
      return;
    }

    const uid = state.user.uid;
    const userRef = doc(db, "users", uid);
    const wdRef = doc(collection(db, "withdrawalRequests"));
    const txRef = doc(collection(db, "users", uid, "walletTransactions"));

    try {
      await runTransaction(db, async (tx) => {
        const userSnap = await tx.get(userRef);
        const curBal = Number(userSnap.data()?.walletBalancePkr || 0);
        if (curBal < amountPkr) throw new Error(`Insufficient balance (${curBal} PKR).`);
        const newBal = curBal - amountPkr;

        tx.set(wdRef, {
          userId: uid,
          inGameName: state.profile.inGameName,
          userEmail: state.profile.email,
          amountPkr,
          paymentMethod: methodKey,
          accountNumber,
          accountTitle,
          status: "pending",
          adminNote: "",
          reviewedBy: "",
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
        tx.update(userRef, {
          walletBalancePkr: newBal,
          updatedAt: serverTimestamp()
        });
        const txPayload = {
          userId: uid,
          type: "withdrawal",
          direction: "debit",
          amountPkr,
          title: `${GAMERZONE_CONFIG.paymentMethods[methodKey].displayName} Withdrawal`,
          subtitle: `To: ${accountNumber} (${accountTitle})`.slice(0, 150),
          referenceId: wdRef.id,
          status: "pending",
          balanceAfterPkr: newBal,
          createdAt: serverTimestamp()
        };
        tx.set(txRef, txPayload);
        tx.set(doc(db, "adminTransactions", txRef.id), {
          ...txPayload,
          inGameName: state.profile.inGameName,
          userEmail: state.profile.email
        });
      });
      closeModal();
      showToast("Withdrawal requested! Funds held pending Admin payout.");
    } catch (err) {
      showToast(err.message || "Withdrawal failed.", true);
    }
  });
}

// Leaderboard
document.querySelectorAll("#lbSwitch .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    state.lbMode = chip.dataset.lb;
    document.querySelectorAll("#lbSwitch .chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderLeaderboard();
  });
});

function renderLeaderboard() {
  if (!state.profile) return;
  if (state.leaderboard.length === 0) {
    el.leaderboardList.innerHTML = `<div class="card"><p class="section-sub">No leaderboard entries yet.</p></div>`;
    return;
  }

  if (state.lbMode === "players") {
    el.leaderboardList.innerHTML = state.leaderboard
      .map(
        (p, idx) => `
      <div class="card">
        <div class="row-between">
          <div>
            <span class="badge ${idx === 0 ? "badge-gold" : "badge-green"}">#${idx + 1}</span>
            <strong style="margin-left:8px;">${escapeHtml(p.inGameName)}</strong>
            <span class="section-sub">(${escapeHtml(p.teamName)}) • UID: ${escapeHtml(p.freeFireUid)}</span>
          </div>
          <div style="text-align:right;">
            <strong style="color:var(--neon-green);">${Number(p.totalPoints || 0)} PTS</strong>
            <div class="section-sub">Match ${Number(p.matchPoints || 0)} + Kills ${Number(p.killPoints || 0)} • ${Number(p.totalWins || 0)} Wins</div>
          </div>
        </div>
      </div>`
      )
      .join("");
  } else {
    const teamMap = new Map();
    state.leaderboard.forEach((p) => {
      const name = p.teamName || "Solo Wolf";
      const cur = teamMap.get(name) || { teamName: name, members: 0, wins: 0, matchPts: 0, killPts: 0, totalPts: 0 };
      cur.members += 1;
      cur.wins += Number(p.totalWins || 0);
      cur.matchPts += Number(p.matchPoints || 0);
      cur.killPts += Number(p.killPoints || 0);
      cur.totalPts += Number(p.totalPoints || 0);
      teamMap.set(name, cur);
    });
    const teams = Array.from(teamMap.values()).sort((a, b) => b.totalPts - a.totalPts);
    el.leaderboardList.innerHTML = teams
      .map(
        (t, idx) => `
      <div class="card">
        <div class="row-between">
          <div>
            <span class="badge ${idx === 0 ? "badge-gold" : "badge-cyan"}">#${idx + 1}</span>
            <strong style="margin-left:8px;">${escapeHtml(t.teamName)}</strong>
            <span class="section-sub">(${t.members} players)</span>
          </div>
          <div style="text-align:right;">
            <strong style="color:var(--cyan-accent);">${t.totalPts} PTS</strong>
            <div class="section-sub">${t.wins} Wins • Match ${t.matchPts} + Kills ${t.killPts}</div>
          </div>
        </div>
      </div>`
      )
      .join("");
  }
}

// Player Dashboard
function renderPlayerDashboard() {
  if (!state.profile) return;
  document.getElementById("statMatches").textContent = Number(state.profile.matchesPlayed || 0);
  document.getElementById("statWins").textContent = Number(state.profile.totalWins || 0);
  document.getElementById("statPoints").textContent = Number(state.profile.totalPoints || 0);

  document.getElementById("profDisplay").value = state.profile.displayName || "";
  document.getElementById("profIgn").value = state.profile.inGameName || "";
  document.getElementById("profUid").value = state.profile.freeFireUid || "";
  document.getElementById("profTeam").value = state.profile.teamName || "";
  document.getElementById("profWa").value = state.profile.whatsappNumber || "";

  el.myMatchesList.innerHTML =
    state.myRegistrations.length === 0
      ? `<p class="section-sub">You haven't joined any tournaments yet.</p>`
      : state.myRegistrations
          .map(
            (r) => `
        <div class="card" style="background:var(--bg-surface);">
          <div class="row-between">
            <strong>${escapeHtml(r.tournamentTitle)}</strong>
            <span class="badge badge-green">Slot #${Number(r.slotNumber || 1)}</span>
          </div>
          <div class="section-sub">${escapeHtml(r.tournamentMode)} • ${escapeHtml(r.mapName)} • ${escapeHtml(r.matchTime)}</div>
        </div>`
          )
          .join("");

  el.announcementsList.innerHTML =
    state.announcements.length === 0
      ? `<p class="section-sub">No announcements right now.</p>`
      : state.announcements
          .map(
            (a) => `
        <div class="card" style="background:var(--bg-surface);">
          <span class="badge badge-cyan">${escapeHtml(a.category)}</span>
          <strong style="display:block; margin-top:6px;">${escapeHtml(a.title)}</strong>
          <p class="section-sub">${escapeHtml(a.message)}</p>
        </div>`
          )
          .join("");
}

document.getElementById("profileUpdateForm")?.addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!state.user) return;
  const displayName = document.getElementById("profDisplay").value.trim();
  const inGameName = document.getElementById("profIgn").value.trim();
  const freeFireUid = document.getElementById("profUid").value.trim();
  const teamName = document.getElementById("profTeam").value.trim();
  const whatsappNumber = document.getElementById("profWa").value.trim();

  try {
    const batch = writeBatch(db);
    batch.update(doc(db, "users", state.user.uid), {
      displayName,
      inGameName,
      freeFireUid,
      teamName,
      whatsappNumber,
      updatedAt: serverTimestamp()
    });
    batch.update(doc(db, "leaderboard", state.user.uid), {
      inGameName,
      freeFireUid,
      teamName,
      updatedAt: serverTimestamp()
    });
    await batch.commit();
    showToast("Profile updated!");
  } catch (err) {
    showToast(err.message || "Failed to update profile.", true);
  }
});

// Admin Dashboard
document.querySelectorAll("#adminSubTabs .chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    state.adminTab = chip.dataset.atab;
    document.querySelectorAll("#adminSubTabs .chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderAdmin();
  });
});

async function loadAdminRoster(tid) {
  state.selectedResultsTournamentId = tid;
  if (!tid) return;
  const snap = await getDocs(collection(db, "tournaments", tid, "registrations"));
  state.selectedTournamentRoster = snap.docs
    .map((d) => ({ id: d.id, ...d.data() }))
    .sort((a, b) => (a.slotNumber || 0) - (b.slotNumber || 0));
  renderAdmin();
}

function renderAdmin() {
  if (!state.isAdmin) return;
  if (state.adminTab === "wallet") {
    const pendingDeps = state.adminDeposits.filter((d) => d.status === "pending");
    const pendingWds = state.adminWithdrawals.filter((w) => w.status === "pending");
    const q = state.adminWalletSearch.trim().toLowerCase();

    const filteredDeposits = state.adminDeposits.filter((d) => {
      const statusOk = !state.adminWalletPendingOnly || d.status === "pending";
      const queryOk =
        !q ||
        (d.transactionId || "").toLowerCase().includes(q) ||
        (d.inGameName || "").toLowerCase().includes(q) ||
        (d.userEmail || "").toLowerCase().includes(q) ||
        (d.senderNumber || "").toLowerCase().includes(q);
      return statusOk && queryOk;
    });

    const filteredWithdrawals = state.adminWithdrawals.filter((w) => {
      const statusOk = !state.adminWalletPendingOnly || w.status === "pending";
      const queryOk =
        !q ||
        (w.accountNumber || "").toLowerCase().includes(q) ||
        (w.accountTitle || "").toLowerCase().includes(q) ||
        (w.inGameName || "").toLowerCase().includes(q) ||
        (w.userEmail || "").toLowerCase().includes(q);
      return statusOk && queryOk;
    });

    const filteredAdminTx = state.adminTransactions.filter((t) => {
      const typeOk =
        state.adminTxTypeFilter === "all" ||
        (state.adminTxTypeFilter === "withdrawal"
          ? t.type === "withdrawal" || t.type === "withdrawal_refund"
          : t.type === state.adminTxTypeFilter);
      const queryOk =
        !q ||
        (t.inGameName || "").toLowerCase().includes(q) ||
        (t.userEmail || "").toLowerCase().includes(q) ||
        (t.title || "").toLowerCase().includes(q) ||
        (t.subtitle || "").toLowerCase().includes(q);
      return typeOk && queryOk;
    });

    let bodyHtml = "";
    if (state.adminWalletSubTab === "deposits") {
      bodyHtml = `
        <div class="list-stack">
          ${
            filteredDeposits.length === 0
              ? `<div class="card"><p class="section-sub">No deposit requests match the selected filter.</p></div>`
              : filteredDeposits
                  .map(
                    (d) => `
              <div class="card card-neon">
                <div class="row-between">
                  <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
                    ${renderStatusBadge(d.status)}
                    <span class="badge badge-cyan">${escapeHtml(d.paymentMethod?.toUpperCase())}</span>
                  </div>
                  <strong style="color:var(--neon-green); font-size:1.15rem;">+${Number(d.amountPkr)} PKR</strong>
                </div>
                <div style="margin-top:8px;"><strong>${escapeHtml(d.inGameName)}</strong> <span class="section-sub">(${escapeHtml(d.userEmail)})</span></div>
                <div class="row-between" style="margin-top:4px;">
                  <div class="section-sub">TID: <span class="mono" style="color:var(--cyan-accent);">${escapeHtml(d.transactionId)}</span> • To: ${escapeHtml(d.receiverAccountNumber)}</div>
                  <button class="btn btn-ghost btn-sm copy-tid-btn" data-tid="${escapeHtml(d.transactionId)}" type="button">Copy TID</button>
                </div>
                <div class="section-sub">Sender: ${escapeHtml(d.senderNumber || "N/A")} • ${escapeHtml(formatTs(d.createdAt))}</div>
                ${d.adminNote ? `<div class="section-sub" style="color:var(--gold-trophy);">Admin Note: ${escapeHtml(d.adminNote)}</div>` : ""}
                ${
                  d.screenshotBase64
                    ? `<div style="margin-top:10px;">
                         <img class="screenshot-thumb view-shot-btn" style="cursor:pointer;" data-depid="${escapeHtml(d.id)}" src="data:image/jpeg;base64,${escapeHtml(d.screenshotBase64)}" alt="Deposit Payment Proof" />
                       </div>`
                    : ""
                }
                <div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap;">
                  ${d.screenshotBase64 ? `<button class="btn btn-ghost btn-sm view-shot-btn" data-depid="${escapeHtml(d.id)}" type="button">Zoom Screenshot</button>` : ""}
                  ${
                    d.status === "pending"
                      ? `<button class="btn btn-primary btn-sm approve-dep-btn" data-id="${escapeHtml(d.id)}" type="button">Approve & Credit</button>
                         <button class="btn btn-danger btn-sm reject-dep-btn" data-id="${escapeHtml(d.id)}" type="button">Reject</button>`
                      : ""
                  }
                </div>
              </div>`
                  )
                  .join("")
          }
        </div>
      `;
    } else if (state.adminWalletSubTab === "withdrawals") {
      bodyHtml = `
        <div class="list-stack">
          ${
            filteredWithdrawals.length === 0
              ? `<div class="card"><p class="section-sub">No withdrawal requests match the selected filter.</p></div>`
              : filteredWithdrawals
                  .map(
                    (w) => `
              <div class="card card-cyan">
                <div class="row-between">
                  <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
                    ${renderStatusBadge(w.status)}
                    <span class="badge badge-gold">${escapeHtml(w.paymentMethod?.toUpperCase())}</span>
                  </div>
                  <strong style="color:var(--gold-trophy); font-size:1.15rem;">${Number(w.amountPkr)} PKR</strong>
                </div>
                <div style="margin-top:8px;"><strong>${escapeHtml(w.inGameName)}</strong> <span class="section-sub">(${escapeHtml(w.userEmail)})</span></div>
                <div class="row-between" style="margin-top:4px;">
                  <div class="section-sub">Pay To: <span class="mono" style="color:var(--cyan-accent);">${escapeHtml(w.accountNumber)}</span> (${escapeHtml(w.accountTitle)})</div>
                  <button class="btn btn-ghost btn-sm copy-tid-btn" data-tid="${escapeHtml(w.accountNumber)}" type="button">Copy Number</button>
                </div>
                <div class="section-sub">${escapeHtml(formatTs(w.createdAt))}</div>
                ${w.adminNote ? `<div class="section-sub" style="color:var(--gold-trophy);">Admin Note: ${escapeHtml(w.adminNote)}</div>` : ""}
                ${
                  w.status === "pending"
                    ? `<div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap;">
                         <button class="btn btn-primary btn-sm approve-wd-btn" data-id="${escapeHtml(w.id)}" type="button">Mark Paid</button>
                         <button class="btn btn-danger btn-sm reject-wd-btn" data-id="${escapeHtml(w.id)}" type="button">Reject & Refund</button>
                       </div>`
                    : ""
                }
              </div>`
                  )
                  .join("")
          }
        </div>
      `;
    } else {
      const txFilters = [
        { key: "all", label: "All Types" },
        { key: "deposit", label: "Deposits" },
        { key: "withdrawal", label: "Withdrawals" },
        { key: "tournament_entry", label: "Match Fees" },
        { key: "prize_payout", label: "Prizes" }
      ];
      bodyHtml = `
        <div class="chip-row" id="adminTxFilters">
          ${txFilters.map((f) => `<button class="chip ${state.adminTxTypeFilter === f.key ? "active" : ""}" data-txtype="${f.key}" type="button">${f.label}</button>`).join("")}
        </div>
        <div class="list-stack">
          ${
            filteredAdminTx.length === 0
              ? `<div class="card"><p class="section-sub">No wallet transactions recorded yet.</p></div>`
              : filteredAdminTx
                  .map((tx) => {
                    const isCredit = tx.direction === "credit";
                    return `
                  <div class="card">
                    <div class="row-between">
                      <div>
                        <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap; margin-bottom:4px;">
                          <span class="badge ${isCredit ? "badge-green" : "badge-crimson"}">${escapeHtml(tx.type)}</span>
                          ${renderStatusBadge(tx.status)}
                        </div>
                        <strong>${escapeHtml(tx.inGameName || tx.userId)}</strong> <span class="section-sub">(${escapeHtml(tx.userEmail || "")})</span>
                        <div class="section-sub">${escapeHtml(tx.title)} — ${escapeHtml(tx.subtitle)}</div>
                        <div class="section-sub">${escapeHtml(formatTs(tx.createdAt))}</div>
                      </div>
                      <div style="text-align:right;">
                        <div style="font-weight:700; font-size:1.1rem; color:${isCredit ? "var(--neon-green)" : "var(--crimson-live)"};">
                          ${isCredit ? "+" : "-"}${Number(tx.amountPkr || 0)} PKR
                        </div>
                        <div class="section-sub">Bal: ${Number(tx.balanceAfterPkr || 0)} PKR</div>
                      </div>
                    </div>
                  </div>`;
                  })
                  .join("")
          }
        </div>
      `;
    }

    el.adminContent.innerHTML = `
      <div class="chip-row" id="adminWalletSubNav">
        <button class="chip ${state.adminWalletSubTab === "deposits" ? "active" : ""}" data-awtab="deposits" type="button">Deposits (${pendingDeps.length} pending)</button>
        <button class="chip ${state.adminWalletSubTab === "withdrawals" ? "active" : ""}" data-awtab="withdrawals" type="button">Withdrawals (${pendingWds.length} pending)</button>
        <button class="chip ${state.adminWalletSubTab === "history" ? "active" : ""}" data-awtab="history" type="button">Complete Transaction History (${state.adminTransactions.length})</button>
      </div>
      <div class="row-between" style="margin-bottom:12px;">
        <input id="adminWalletSearchInput" class="input" style="max-width:420px;" placeholder="Search TID, IGN, email, or account number..." value="${escapeHtml(state.adminWalletSearch)}" />
        ${
          state.adminWalletSubTab !== "history"
            ? `<button id="toggleAdminPendingBtn" class="chip ${state.adminWalletPendingOnly ? "active" : ""}" type="button">${state.adminWalletPendingOnly ? "Showing: Pending Only" : "Showing: All (Pending, Approved, Rejected)"}</button>`
            : ""
        }
      </div>
      ${bodyHtml}
    `;

    el.adminContent.querySelectorAll("#adminWalletSubNav .chip").forEach((b) =>
      b.addEventListener("click", () => {
        state.adminWalletSubTab = b.dataset.awtab;
        renderAdmin();
      })
    );
    document.getElementById("toggleAdminPendingBtn")?.addEventListener("click", () => {
      state.adminWalletPendingOnly = !state.adminWalletPendingOnly;
      renderAdmin();
    });
    const searchEl = document.getElementById("adminWalletSearchInput");
    searchEl?.addEventListener("input", (e) => {
      state.adminWalletSearch = e.target.value;
      const pos = e.target.selectionStart;
      renderAdmin();
      const refocused = document.getElementById("adminWalletSearchInput");
      if (refocused) {
        refocused.focus();
        refocused.setSelectionRange(pos, pos);
      }
    });
    el.adminContent.querySelectorAll("#adminTxFilters .chip").forEach((b) =>
      b.addEventListener("click", () => {
        state.adminTxTypeFilter = b.dataset.txtype;
        renderAdmin();
      })
    );
    el.adminContent.querySelectorAll(".copy-tid-btn").forEach((b) =>
      b.addEventListener("click", () => copyText(b.dataset.tid, "Copied"))
    );

    bindScreenshotButtons();
    el.adminContent.querySelectorAll(".approve-dep-btn").forEach((b) =>
      b.addEventListener("click", () => reviewDeposit(b.dataset.id, true))
    );
    el.adminContent.querySelectorAll(".reject-dep-btn").forEach((b) =>
      b.addEventListener("click", () => reviewDeposit(b.dataset.id, false))
    );
    el.adminContent.querySelectorAll(".approve-wd-btn").forEach((b) =>
      b.addEventListener("click", () => reviewWithdrawal(b.dataset.id, true))
    );
    el.adminContent.querySelectorAll(".reject-wd-btn").forEach((b) =>
      b.addEventListener("click", () => reviewWithdrawal(b.dataset.id, false))
    );
  } else if (state.adminTab === "tournaments") {
    el.adminContent.innerHTML = `
      <div style="margin-bottom:14px;">
        <button id="createTourneyBtn" class="btn btn-primary" type="button">+ Create Tournament</button>
      </div>
      <div class="grid grid-2">
        ${state.tournaments
          .map(
            (t) => `
          <div class="card">
            <div class="row-between">
              <strong>${escapeHtml(t.title)}</strong>
              <span class="badge badge-green">${escapeHtml(t.status)}</span>
            </div>
            <div class="section-sub">${escapeHtml(t.mode)} • ${escapeHtml(t.mapName)} • Fee ${Number(t.entryFeePkr)} PKR • ${Number(t.registeredCount)}/${Number(t.totalSlots)} slots</div>
            <div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap;">
              <button class="btn btn-cyan btn-sm edit-tourney-btn" data-id="${escapeHtml(t.id)}" type="button">Edit / Room</button>
              <button class="btn btn-danger btn-sm del-tourney-btn" data-id="${escapeHtml(t.id)}" type="button">Delete</button>
            </div>
          </div>`
          )
          .join("")}
      </div>
    `;
    document.getElementById("createTourneyBtn").addEventListener("click", () => openTournamentEditorModal(null));
    el.adminContent.querySelectorAll(".edit-tourney-btn").forEach((b) =>
      b.addEventListener("click", () => openTournamentEditorModal(state.tournaments.find((t) => t.id === b.dataset.id)))
    );
    el.adminContent.querySelectorAll(".del-tourney-btn").forEach((b) =>
      b.addEventListener("click", async () => {
        if (!confirm("Delete this tournament?")) return;
        try {
          await deleteDoc(doc(db, "tournaments", b.dataset.id));
          showToast("Tournament deleted.");
        } catch (err) {
          showToast(err.message, true);
        }
      })
    );
  } else if (state.adminTab === "results") {
    el.adminContent.innerHTML = `
      <div class="field" style="max-width:420px;">
        <label for="resTourneySel">Select Tournament</label>
        <select id="resTourneySel" class="select">
          ${state.tournaments.map((t) => `<option value="${escapeHtml(t.id)}" ${t.id === state.selectedResultsTournamentId ? "selected" : ""}>${escapeHtml(t.title)} (${Number(t.registeredCount)}/${Number(t.totalSlots)})</option>`).join("")}
        </select>
      </div>
      <div style="margin-bottom:12px;">
        <button id="loadRosterBtn" class="btn btn-cyan btn-sm" type="button">Load Registered Players</button>
      </div>
      <div class="list-stack">
        ${
          state.selectedTournamentRoster.length === 0
            ? `<div class="card"><p class="section-sub">Click "Load Registered Players" to score participants & award PKR prizes.</p></div>`
            : state.selectedTournamentRoster
                .map(
                  (r) => `
              <div class="card">
                <div class="row-between">
                  <div>
                    <strong>Slot #${Number(r.slotNumber)} • ${escapeHtml(r.inGameName)}</strong>
                    <div class="section-sub">UID: ${escapeHtml(r.freeFireUid)} • ${escapeHtml(r.teamName)} • Current: ${Number(r.totalPoints)} PTS</div>
                  </div>
                  <button class="btn btn-gold btn-sm score-reg-btn" data-uid="${escapeHtml(r.userId)}" type="button">Score / Prize</button>
                </div>
              </div>`
                )
                .join("")
        }
      </div>
    `;
    document.getElementById("loadRosterBtn")?.addEventListener("click", () => {
      loadAdminRoster(document.getElementById("resTourneySel").value);
    });
    el.adminContent.querySelectorAll(".score-reg-btn").forEach((b) => {
      b.addEventListener("click", () => {
        const reg = state.selectedTournamentRoster.find((x) => x.userId === b.dataset.uid);
        const tourney = state.tournaments.find((x) => x.id === state.selectedResultsTournamentId);
        if (reg && tourney) openScorePlayerModal(tourney, reg);
      });
    });
  } else {
    el.adminContent.innerHTML = `
      <div class="card card-neon" style="margin-bottom:14px;">
        <h3 style="margin-bottom:10px;">Publish Global Announcement</h3>
        <form id="annForm">
          <div class="grid grid-2">
            <div class="field"><label for="annTitle">Title</label><input id="annTitle" class="input" required minlength="3" maxlength="100" /></div>
            <div class="field">
              <label for="annCat">Category</label>
              <select id="annCat" class="select">
                <option value="tournament">Tournament</option>
                <option value="winner">Winner</option>
                <option value="alert">Alert</option>
                <option value="rules">Rules</option>
              </select>
            </div>
          </div>
          <div class="field"><label for="annMsg">Message</label><textarea id="annMsg" class="textarea" required minlength="5" maxlength="1000"></textarea></div>
          <button class="btn btn-primary" type="submit">Publish Announcement</button>
        </form>
      </div>
      <div class="list-stack">
        ${state.announcements
          .map(
            (a) => `
          <div class="card">
            <div class="row-between">
              <div><strong>${escapeHtml(a.title)}</strong><p class="section-sub">${escapeHtml(a.message)}</p></div>
              <button class="btn btn-danger btn-sm del-ann-btn" data-id="${escapeHtml(a.id)}" type="button">Delete</button>
            </div>
          </div>`
          )
          .join("")}
      </div>
    `;
    document.getElementById("annForm").addEventListener("submit", async (e) => {
      e.preventDefault();
      try {
        await setDoc(doc(collection(db, "announcements")), {
          title: document.getElementById("annTitle").value.trim(),
          category: document.getElementById("annCat").value,
          message: document.getElementById("annMsg").value.trim(),
          createdBy: state.user.uid,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
        showToast("Announcement published!");
        e.target.reset();
      } catch (err) {
        showToast(err.message, true);
      }
    });
    el.adminContent.querySelectorAll(".del-ann-btn").forEach((b) =>
      b.addEventListener("click", async () => {
        try {
          await deleteDoc(doc(db, "announcements", b.dataset.id));
          showToast("Announcement deleted.");
        } catch (err) {
          showToast(err.message || "Failed to delete announcement.", true);
        }
      })
    );
  }
}

function openAdminDecisionModal({ title, summary, defaultNote, confirmLabel, confirmClass, onConfirm }) {
  openModal(`
    <div class="row-between" style="margin-bottom:12px;">
      <h3>${escapeHtml(title)}</h3>
      <button class="btn btn-ghost btn-sm" id="closeDecisionModal" type="button">Cancel</button>
    </div>
    <p class="section-sub" style="margin-bottom:12px;">${escapeHtml(summary)}</p>
    <form id="adminDecisionForm">
      <div class="field">
        <label for="decisionNote">Admin Note</label>
        <input id="decisionNote" class="input" maxlength="300" value="${escapeHtml(defaultNote)}" required />
      </div>
      <div style="display:flex; gap:10px; justify-content:flex-end; flex-wrap:wrap;">
        <button class="btn btn-ghost" id="cancelDecisionBtn" type="button">Cancel</button>
        <button class="btn ${escapeHtml(confirmClass)}" id="confirmDecisionBtn" type="submit">${escapeHtml(confirmLabel)}</button>
      </div>
    </form>
  `);
  document.getElementById("closeDecisionModal")?.addEventListener("click", closeModal);
  document.getElementById("cancelDecisionBtn")?.addEventListener("click", closeModal);
  document.getElementById("adminDecisionForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const note = document.getElementById("decisionNote").value.trim() || defaultNote;
    const submitBtn = document.getElementById("confirmDecisionBtn");
    if (submitBtn) submitBtn.disabled = true;
    closeModal();
    await onConfirm(note);
  });
}

async function reviewDeposit(reqId, approve) {
  const req = state.adminDeposits.find((d) => d.id === reqId);
  if (!req) return;
  openAdminDecisionModal({
    title: approve ? "Approve Deposit & Credit Wallet" : "Reject Deposit Request",
    summary: `${req.inGameName || "Player"} • ${Number(req.amountPkr)} PKR • TID: ${req.transactionId}`,
    defaultNote: approve ? "Verified by Admin" : "Invalid TID or unclear screenshot",
    confirmLabel: approve ? "Approve & Credit" : "Reject Deposit",
    confirmClass: approve ? "btn-primary" : "btn-danger",
    onConfirm: async (note) => {
      try {
        await runTransaction(db, async (tx) => {
          const depRef = doc(db, "depositRequests", req.id);
          const userRef = doc(db, "users", req.userId);
          const txRef = doc(collection(db, "users", req.userId, "walletTransactions"));
          const depSnap = await tx.get(depRef);
          if (!depSnap.exists() || depSnap.data().status !== "pending") throw new Error("Already processed.");
          const userSnap = await tx.get(userRef);
          const curBal = Number(userSnap.data()?.walletBalancePkr || 0);

          tx.update(depRef, {
            status: approve ? "approved" : "rejected",
            adminNote: note.slice(0, 300),
            reviewedBy: state.user.uid,
            updatedAt: serverTimestamp()
          });
          if (approve) {
            const newBal = curBal + Number(req.amountPkr);
            tx.update(userRef, { walletBalancePkr: newBal, updatedAt: serverTimestamp() });
            const txPayload = {
              userId: req.userId,
              type: "deposit",
              direction: "credit",
              amountPkr: Number(req.amountPkr),
              title: `${GAMERZONE_CONFIG.paymentMethods[req.paymentMethod]?.displayName || "Wallet"} Deposit Approved`,
              subtitle: `TID: ${req.transactionId}`.slice(0, 150),
              referenceId: req.id,
              status: "completed",
              balanceAfterPkr: newBal,
              createdAt: serverTimestamp()
            };
            tx.set(txRef, txPayload);
            tx.set(doc(db, "adminTransactions", txRef.id), {
              ...txPayload,
              inGameName: req.inGameName || "",
              userEmail: req.userEmail || ""
            });
          }
        });
        showToast(approve ? "Deposit approved & balance credited!" : "Deposit rejected.");
      } catch (err) {
        showToast(err.message, true);
      }
    }
  });
}

async function reviewWithdrawal(reqId, approve) {
  const req = state.adminWithdrawals.find((w) => w.id === reqId);
  if (!req) return;
  openAdminDecisionModal({
    title: approve ? "Confirm Withdrawal Payout" : "Reject & Refund Withdrawal",
    summary: `${req.inGameName || "Player"} • ${Number(req.amountPkr)} PKR • ${req.accountNumber}`,
    defaultNote: approve ? "Paid by Admin" : "Account details mismatch",
    confirmLabel: approve ? "Mark Paid" : "Reject & Refund",
    confirmClass: approve ? "btn-primary" : "btn-danger",
    onConfirm: async (note) => {
      try {
        await runTransaction(db, async (tx) => {
          const wdRef = doc(db, "withdrawalRequests", req.id);
          const userRef = doc(db, "users", req.userId);
          const txRef = doc(collection(db, "users", req.userId, "walletTransactions"));
          const wdSnap = await tx.get(wdRef);
          if (!wdSnap.exists() || wdSnap.data().status !== "pending") throw new Error("Already processed.");
          const userSnap = await tx.get(userRef);
          const curBal = Number(userSnap.data()?.walletBalancePkr || 0);

          tx.update(wdRef, {
            status: approve ? "approved" : "rejected",
            adminNote: note.slice(0, 300),
            reviewedBy: state.user.uid,
            updatedAt: serverTimestamp()
          });
          if (approve) {
            const txPayload = {
              userId: req.userId,
              type: "withdrawal",
              direction: "debit",
              amountPkr: Number(req.amountPkr),
              title: `${GAMERZONE_CONFIG.paymentMethods[req.paymentMethod]?.displayName || "Wallet"} Payout Sent`,
              subtitle: `To: ${req.accountNumber} • ${note}`.slice(0, 150),
              referenceId: req.id,
              status: "completed",
              balanceAfterPkr: curBal,
              createdAt: serverTimestamp()
            };
            tx.set(txRef, txPayload);
            tx.set(doc(db, "adminTransactions", txRef.id), {
              ...txPayload,
              inGameName: req.inGameName || "",
              userEmail: req.userEmail || ""
            });
          } else {
            const refunded = curBal + Number(req.amountPkr);
            tx.update(userRef, { walletBalancePkr: refunded, updatedAt: serverTimestamp() });
            const txPayload = {
              userId: req.userId,
              type: "withdrawal_refund",
              direction: "credit",
              amountPkr: Number(req.amountPkr),
              title: "Withdrawal Refunded",
              subtitle: note.slice(0, 150),
              referenceId: req.id,
              status: "completed",
              balanceAfterPkr: refunded,
              createdAt: serverTimestamp()
            };
            tx.set(txRef, txPayload);
            tx.set(doc(db, "adminTransactions", txRef.id), {
              ...txPayload,
              inGameName: req.inGameName || "",
              userEmail: req.userEmail || ""
            });
          }
        });
        showToast(approve ? "Withdrawal marked paid!" : "Withdrawal rejected & refunded.");
      } catch (err) {
        showToast(err.message, true);
      }
    }
  });
}

function openTournamentEditorModal(existing) {
  openModal(`
    <div class="row-between" style="margin-bottom:12px;">
      <h3>${existing ? "Edit Tournament" : "Create Tournament"}</h3>
      <button class="btn btn-ghost btn-sm" id="closeTModal" type="button">Close</button>
    </div>
    <form id="tourneyForm">
      <div class="field"><label>Title</label><input id="tTitle" class="input" required minlength="3" maxlength="80" value="${escapeHtml(existing?.title || "")}" /></div>
      <div class="grid grid-2">
        <div class="field">
          <label>Mode</label>
          <select id="tMode" class="select">
            ${["Solo", "Duo", "Squad", "Clash Squad", "Battle Royale"].map((m) => `<option ${existing?.mode === m ? "selected" : ""}>${m}</option>`).join("")}
          </select>
        </div>
        <div class="field">
          <label>Status</label>
          <select id="tStatus" class="select">
            ${["upcoming", "live", "completed"].map((s) => `<option value="${s}" ${existing?.status === s ? "selected" : ""}>${s}</option>`).join("")}
          </select>
        </div>
      </div>
      <div class="grid grid-2">
        <div class="field"><label>Map</label><input id="tMap" class="input" required value="${escapeHtml(existing?.mapName || "Bermuda")}" /></div>
        <div class="field"><label>Match Time</label><input id="tTime" class="input" required value="${escapeHtml(existing?.matchTime || "Tonight 9:00 PM PKT")}" /></div>
      </div>
      <div class="grid grid-3">
        <div class="field"><label>Entry Fee (PKR)</label><input id="tFee" class="input" type="number" min="0" value="${Number(existing?.entryFeePkr ?? 50)}" /></div>
        <div class="field"><label>Prize Pool</label><input id="tPool" class="input" type="number" min="0" value="${Number(existing?.prizePoolPkr ?? 1750)}" /></div>
        <div class="field"><label>Slots (2-100)</label><input id="tSlots" class="input" type="number" min="2" max="100" value="${Number(existing?.totalSlots ?? 48)}" /></div>
      </div>
      <div class="grid grid-3">
        <div class="field"><label>1st Prize</label><input id="tP1" class="input" type="number" min="0" value="${Number(existing?.firstPrizePkr ?? 1000)}" /></div>
        <div class="field"><label>2nd Prize</label><input id="tP2" class="input" type="number" min="0" value="${Number(existing?.secondPrizePkr ?? 500)}" /></div>
        <div class="field"><label>3rd Prize</label><input id="tP3" class="input" type="number" min="0" value="${Number(existing?.thirdPrizePkr ?? 250)}" /></div>
      </div>
      <div class="grid grid-2">
        <div class="field"><label>Room ID</label><input id="tRoomId" class="input" value="${escapeHtml(existing?.roomId || "")}" /></div>
        <div class="field"><label>Room Password</label><input id="tRoomPass" class="input" value="${escapeHtml(existing?.roomPassword || "")}" /></div>
      </div>
      <div class="field"><label>Rules</label><textarea id="tRules" class="textarea" required>${escapeHtml(existing?.rules || "Mobile only. No emulators or hacks. Join room 10 mins early.")}</textarea></div>
      <button class="btn btn-primary" style="width:100%;" type="submit">${existing ? "Save Changes" : "Publish Tournament"}</button>
    </form>
  `);
  document.getElementById("closeTModal").addEventListener("click", closeModal);
  document.getElementById("tourneyForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
      title: document.getElementById("tTitle").value.trim(),
      mode: document.getElementById("tMode").value,
      status: document.getElementById("tStatus").value,
      mapName: document.getElementById("tMap").value.trim(),
      matchTime: document.getElementById("tTime").value.trim(),
      entryFeePkr: parseInt(document.getElementById("tFee").value || "0", 10),
      prizePoolPkr: parseInt(document.getElementById("tPool").value || "0", 10),
      totalSlots: parseInt(document.getElementById("tSlots").value || "48", 10),
      firstPrizePkr: parseInt(document.getElementById("tP1").value || "0", 10),
      secondPrizePkr: parseInt(document.getElementById("tP2").value || "0", 10),
      thirdPrizePkr: parseInt(document.getElementById("tP3").value || "0", 10),
      roomId: document.getElementById("tRoomId").value.trim(),
      roomPassword: document.getElementById("tRoomPass").value.trim(),
      rules: document.getElementById("tRules").value.trim(),
      updatedAt: serverTimestamp()
    };

    try {
      if (existing) {
        await updateDoc(doc(db, "tournaments", existing.id), payload);
      } else {
        await setDoc(doc(collection(db, "tournaments")), {
          ...payload,
          registeredCount: 0,
          winnerName: "",
          winnerUid: "",
          createdBy: state.user.uid,
          createdAt: serverTimestamp()
        });
      }
      closeModal();
      showToast(existing ? "Tournament updated!" : "Tournament created!");
    } catch (err) {
      showToast(err.message, true);
    }
  });
}

function openScorePlayerModal(tourney, reg) {
  openModal(`
    <div class="row-between" style="margin-bottom:12px;">
      <h3>Score ${escapeHtml(reg.inGameName)}</h3>
      <button class="btn btn-ghost btn-sm" id="closeSModal" type="button">Close</button>
    </div>
    <form id="scoreForm">
      <div class="grid grid-3">
        <div class="field"><label>Placement #</label><input id="sPlace" class="input" type="number" min="1" max="100" value="${Number(reg.placement || 1)}" /></div>
        <div class="field"><label>Match Pts</label><input id="sMatch" class="input" type="number" min="0" value="${Number(reg.matchPoints || 0)}" /></div>
        <div class="field"><label>Kill Pts</label><input id="sKill" class="input" type="number" min="0" value="${Number(reg.killPoints || 0)}" /></div>
      </div>
      <div class="field"><label>Credit Prize Payout to Wallet (PKR, optional)</label><input id="sPrize" class="input" type="number" min="0" value="0" /></div>
      <div class="field">
        <label><input id="sWinner" type="checkbox" /> Mark as Tournament Winner & Complete Match</label>
      </div>
      <button class="btn btn-gold" style="width:100%;" type="submit">Publish Result & Credit Prize</button>
    </form>
  `);
  document.getElementById("closeSModal").addEventListener("click", closeModal);
  document.getElementById("scoreForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const placement = parseInt(document.getElementById("sPlace").value || "1", 10);
    const matchPoints = parseInt(document.getElementById("sMatch").value || "0", 10);
    const killPoints = parseInt(document.getElementById("sKill").value || "0", 10);
    const prize = parseInt(document.getElementById("sPrize").value || "0", 10);
    const isWinner = document.getElementById("sWinner").checked;
    const totalPoints = matchPoints + killPoints;
    const dMatch = matchPoints - Number(reg.matchPoints || 0);
    const dKill = killPoints - Number(reg.killPoints || 0);
    const dTotal = totalPoints - Number(reg.totalPoints || 0);
    const firstScore = Number(reg.placement || 0) === 0 && placement > 0;

    try {
      await runTransaction(db, async (tx) => {
        const tRef = doc(db, "tournaments", tourney.id);
        const tRegRef = doc(db, "tournaments", tourney.id, "registrations", reg.userId);
        const uRef = doc(db, "users", reg.userId);
        const uRegRef = doc(db, "users", reg.userId, "registrations", tourney.id);
        const lbRef = doc(db, "leaderboard", reg.userId);
        const userSnap = prize > 0 ? await tx.get(uRef) : null;
        const curBal = Number(userSnap?.data()?.walletBalancePkr || 0);

        const regUpd = {
          matchPoints,
          killPoints,
          totalPoints,
          placement,
          tournamentStatus: isWinner ? "completed" : tourney.status,
          updatedAt: serverTimestamp()
        };
        tx.update(tRegRef, regUpd);
        tx.update(uRegRef, regUpd);

        const statUpd = {
          matchPoints: increment(dMatch),
          killPoints: increment(dKill),
          totalPoints: increment(dTotal),
          updatedAt: serverTimestamp()
        };
        if (firstScore) statUpd.matchesPlayed = increment(1);
        if (isWinner) statUpd.totalWins = increment(1);
        tx.update(uRef, statUpd);
        tx.update(lbRef, statUpd);

        if (isWinner) {
          tx.update(tRef, {
            winnerName: reg.inGameName,
            winnerUid: reg.userId,
            status: "completed",
            updatedAt: serverTimestamp()
          });
        }
        if (prize > 0) {
          const newBal = curBal + prize;
          tx.update(uRef, { walletBalancePkr: newBal, updatedAt: serverTimestamp() });
          const prizeTxRef = doc(collection(db, "users", reg.userId, "walletTransactions"));
          const prizePayload = {
            userId: reg.userId,
            type: "prize_payout",
            direction: "credit",
            amountPkr: prize,
            title: `Prize Won: ${tourney.title}`.slice(0, 100),
            subtitle: `Placement #${placement} • ${matchPoints + killPoints} PTS`,
            referenceId: tourney.id,
            status: "completed",
            balanceAfterPkr: newBal,
            createdAt: serverTimestamp()
          };
          tx.set(prizeTxRef, prizePayload);
          tx.set(doc(db, "adminTransactions", prizeTxRef.id), {
            ...prizePayload,
            inGameName: reg.inGameName || "",
            userEmail: userSnap?.data()?.email || ""
          });
        }
      });
      closeModal();
      showToast("Match result published!");
      loadAdminRoster(tourney.id);
    } catch (err) {
      showToast(err.message, true);
    }
  });
}

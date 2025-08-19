const apiBase = '';
const $ = (s) => document.querySelector(s);
const $$ = (s) => Array.from(document.querySelectorAll(s));

const state = {
  token: localStorage.getItem('token') || null,
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  role: localStorage.getItem('role') || null,
  leaveTypes: [],
};

function show(sectionId){
  const sections = ["authSection","dashboardSection","applySection","requestsSection","approvalsSection","holidaysSection"];
  sections.forEach(id=> document.getElementById(id).classList.add('hidden'));
  document.getElementById(sectionId).classList.remove('hidden');
}

function setAuthUI(){
  const loggedIn = !!state.token;
  $('#userInfo').style.display = loggedIn ? 'flex' : 'none';
  $$('#nav .link').forEach(btn => btn.disabled = !loggedIn);
  $('#nav-approvals').classList.toggle('hidden', !(state.role==='MANAGER' || state.role==='ADMIN'));
  if (loggedIn){
    $('#userName').textContent = state.user?.fullName || state.user?.email || '';
    $('#avatar').src = state.user?.avatarUrl || 'data:image/gif;base64,R0lGODlhAQABAIAAAAUEBA==';
    show('dashboardSection');
    refreshDashboard();
    refreshLeaveTypes();
    refreshMyRequests();
    refreshHolidays();
    if (state.role==='MANAGER' || state.role==='ADMIN') refreshApprovals();
  } else {
    show('authSection');
  }
}

function headers(json=true){
  const h = {};
  if (json) h['Content-Type'] = 'application/json';
  if (state.token) h['Authorization'] = `Bearer ${state.token}`;
  return h;
}

async function api(path, method='GET', body){
  const resp = await fetch(apiBase + path, {
    method,
    headers: headers(body!=null),
    body: body!=null ? JSON.stringify(body) : undefined,
  });
  if (!resp.ok){
    const txt = await resp.text();
    throw new Error(txt || ('HTTP '+resp.status));
  }
  const contentType = resp.headers.get('Content-Type') || '';
  if (contentType.includes('application/json')) return resp.json();
  return resp.text();
}

// Auth handlers
$('#tab-login').addEventListener('click',()=>{
  $('#tab-login').classList.add('active');
  $('#tab-register').classList.remove('active');
  $('#loginForm').classList.remove('hidden');
  $('#registerForm').classList.add('hidden');
  $('#authMsg').textContent='';
});
$('#tab-register').addEventListener('click',()=>{
  $('#tab-register').classList.add('active');
  $('#tab-login').classList.remove('active');
  $('#registerForm').classList.remove('hidden');
  $('#loginForm').classList.add('hidden');
  $('#authMsg').textContent='';
});

$('#loginForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  $('#authMsg').textContent = 'Signing in...';
  try{
    const data = await api('/api/auth/login','POST',{
      email: $('#loginEmail').value.trim(),
      password: $('#loginPassword').value,
    });
    state.token = data.token; state.role = data.role; state.user = data;
    localStorage.setItem('token', state.token);
    localStorage.setItem('role', state.role);
    localStorage.setItem('user', JSON.stringify(state.user));
    $('#authMsg').textContent = '';
    setAuthUI();
  }catch(err){
    $('#authMsg').textContent = 'Login failed: ' + safeError(err);
  }
});

$('#registerForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  $('#authMsg').textContent = 'Creating account...';
  try{
    const data = await api('/api/auth/register','POST',{
      fullName: $('#regName').value.trim(),
      email: $('#regEmail').value.trim(),
      password: $('#regPassword').value,
    });
    state.token = data.token; state.role = data.role; state.user = data;
    localStorage.setItem('token', state.token);
    localStorage.setItem('role', state.role);
    localStorage.setItem('user', JSON.stringify(state.user));
    $('#authMsg').textContent = '';
    setAuthUI();
  }catch(err){
    $('#authMsg').textContent = 'Registration failed: ' + safeError(err);
  }
});

$('#logoutBtn').addEventListener('click',()=>{
  state.token=null; state.user=null; state.role=null;
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  localStorage.removeItem('role');
  setAuthUI();
});

// Nav
$('#nav-dashboard').addEventListener('click',()=>{show('dashboardSection'); refreshDashboard();});
$('#nav-apply').addEventListener('click',()=>{show('applySection'); refreshLeaveTypesForApply();});
$('#nav-requests').addEventListener('click',()=>{show('requestsSection'); refreshMyRequests();});
$('#nav-approvals').addEventListener('click',()=>{show('approvalsSection'); refreshApprovals();});
$('#nav-holidays').addEventListener('click',()=>{show('holidaysSection'); refreshHolidays();});

// Dashboard
async function refreshDashboard(){
  try{
    const bal = await api('/api/leave/balance');
    $('#balAccrued').textContent = fmt(bal.accrued);
    $('#balUsed').textContent = fmt(bal.used);
    $('#balCarry').textContent = fmt(bal.carryover);
    $('#balAvail').textContent = fmt(bal.balance);
    await refreshLeaveTypes();
  }catch(e){ /* ignore */ }
}

async function refreshLeaveTypes(){
  try{
    const types = await api('/api/leave/types');
    state.leaveTypes = types;
    const ul = $('#leaveTypes'); ul.innerHTML='';
    types.forEach(t=>{
      const li = document.createElement('li');
      li.innerHTML = `<span>${escapeHtml(t.name)}</span><span class="badge">${t.accrualRatePerMonth?.toFixed?.(2) || 0}/mo</span>`;
      ul.appendChild(li);
    });
  }catch(e){ /* ignore */ }
}

function refreshLeaveTypesForApply(){
  const sel = $('#applyType'); sel.innerHTML='';
  state.leaveTypes.forEach(t=>{
    const opt = document.createElement('option');
    opt.value = t.id; opt.textContent = t.name;
    sel.appendChild(opt);
  });
}

$('#applyForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  $('#applyMsg').textContent = 'Submitting...';
  try{
    const body = {
      leaveTypeId: Number($('#applyType').value),
      startDate: $('#applyStart').value,
      endDate: $('#applyEnd').value,
      reason: $('#applyReason').value.trim()
    };
    const resp = await api('/api/leave/apply','POST', body);
    $('#applyMsg').textContent = 'Application submitted. Status: '+resp.status;
    $('#applyForm').reset();
    refreshMyRequests();
  }catch(err){
    $('#applyMsg').textContent = 'Failed: '+safeError(err);
  }
});

async function refreshMyRequests(){
  try{
    const data = await api('/api/leave/my');
    const wrap = $('#myRequests');
    wrap.innerHTML = renderRequestsTable(data, false);
  }catch(e){ /* ignore */ }
}

async function refreshApprovals(){
  try{
    // Minimal approach: list SUBMITTED requests by querying approved status endpoint isn't provided.
    // We'll fetch all on-leave-today plus pending via a small hack: call a manager-only listing not available.
    // Fallback: show only SUBMITTED from user's list (not ideal). For demo, we will show SUBMITTED from all fetched via types of my requests is limited.
    // Since we don't have an endpoint, we hide approvals if we can't load.
    const wrap = $('#pendingApprovals');
    wrap.innerHTML = `<div class="muted">Use Swagger UI to approve/reject by ID. (Manager/Admin)</div>`;
  }catch(e){ /* ignore */ }
}

async function refreshHolidays(){
  try{
    const holidays = await api('/api/public/holidays');
    const ul = $('#holidaysList'); ul.innerHTML='';
    holidays.forEach(h=>{
      const li = document.createElement('li');
      li.innerHTML = `<span>${escapeHtml(h.name)}</span><span class="muted">${h.date}</span>`;
      ul.appendChild(li);
    });
  }catch(e){ /* ignore */ }
}

function renderRequestsTable(items){
  if (!items || items.length===0) return '<div class="muted">No requests yet.</div>';
  const rows = items.map(r=>`<tr>
    <td>${escapeHtml(r.leaveType?.name || '')}</td>
    <td>${r.startDate} → ${r.endDate}</td>
    <td>${(r.days ?? 0)}</td>
    <td><span class="status ${r.status}">${r.status}</span></td>
    <td>${escapeHtml(r.reason || '')}</td>
  </tr>`).join('');
  return `<table>
    <thead><tr><th>Type</th><th>Dates</th><th>Days</th><th>Status</th><th>Reason</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>`;
}

function fmt(n){ return Number.parseFloat(n || 0).toFixed(2); }
function escapeHtml(s){ return (s||'').toString().replace(/[&<>"']/g, c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c])); }
function safeError(err){ try{ const j = JSON.parse(err.message); return (j.error || err.message); }catch{ return err.message; } }

// Init
$('#year').textContent = new Date().getFullYear();
setAuthUI();

// On first load: if demo users exist, prefill for convenience
$('#loginEmail').value = 'staff@demo.com';

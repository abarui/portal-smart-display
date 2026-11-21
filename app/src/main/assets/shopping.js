'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const storageKey = 'portal-shopping-key';
  let token = new URLSearchParams(location.hash.slice(1)).get('key') || '';
  try { if (token) localStorage.setItem(storageKey, token); else token = localStorage.getItem(storageKey) || ''; } catch (_) {}
  if (location.hash) history.replaceState(null, '', location.pathname);
  let lists = [], selected = '', signature = '', busy = false, loading = false, revision = 0;
  const showError = message => { $('error').textContent = message; $('error').hidden = !message; };
  async function request(data) {
    const response = await fetch('/api/shopping', {
      method: data ? 'POST' : 'GET', cache: 'no-store',
      headers: { 'X-Portal-Token': token, ...(data ? { 'Content-Type': 'application/json' } : {}) },
      ...(data ? { body: JSON.stringify(data) } : {})
    });
    const value = await response.json();
    if (!response.ok) throw new Error(value.error || 'Could not update your list. Please try again.');
    return value;
  }
  function render() {
    if (!lists.some(list => list.id === selected)) selected = lists[0]?.id || '';
    const old = $('lists').value;
    $('lists').replaceChildren(...lists.map(list => {
      const option = document.createElement('option'); option.value = list.id; option.textContent = list.name; return option;
    }));
    $('lists').value = selected || old;
    const list = lists.find(list => list.id === selected);
    $('title').textContent = list?.name || 'Create your first list';
    $('add-item').hidden = !list;
    $('count').textContent = list ? `${list.items.filter(item => !item.done).length} to get` : '';
    $('items').replaceChildren();
    if (!list?.items.length) {
      const empty = document.createElement('p'); empty.className = 'empty';
      empty.textContent = list ? 'Your list is ready. Add the first item above.' : 'Use “Create another list” below to get started.';
      $('items').append(empty);
    }
    [...(list?.items || [])].sort((a,b) => Number(a.done)-Number(b.done)).forEach(item => {
      const row = document.createElement('div'); row.className = `item${item.done ? ' done' : ''}`;
      const label = document.createElement('label');
      const check = document.createElement('input'); check.type = 'checkbox'; check.checked = item.done; check.disabled = busy;
      check.addEventListener('change', () => mutate({action:'setItemDone',listId:selected,itemId:item.id,done:check.checked}));
      const text = document.createElement('span'); text.textContent = item.text;
      label.append(check,text);
      const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'remove'; remove.textContent = '×';
      remove.setAttribute('aria-label', `Remove ${item.text}`); remove.disabled = busy;
      remove.addEventListener('click', () => mutate({action:'deleteItem',listId:selected,itemId:item.id}));
      row.append(label,remove); $('items').append(row);
    });
    $('app').hidden = false;
  }
  async function refresh() {
    if (busy || loading || document.hidden) return;
    loading = true;
    const requestedRevision = revision;
    try {
      const data = await request();
      // A mutation may have started while this GET was in flight.
      if (busy || requestedRevision !== revision) return;
      const next = JSON.stringify(data.lists);
      lists = data.lists;
      if (next !== signature) { signature = next; render(); }
      $('status').textContent = 'Connected · Your lists are up to date'; showError('');
    } catch (error) { $('status').textContent = 'Not connected'; showError(error.message || 'Reconnect to your home Wi-Fi and try again.'); }
    finally { loading = false; }
  }
  async function mutate(data, completed) {
    if (busy) return;
    busy = true;
    revision += 1;
    document.querySelectorAll('button').forEach(button => button.disabled = true);
    try {
      const value = await request(data); lists = value.lists; signature = JSON.stringify(lists);
      if (data.action === 'addList') selected = lists[lists.length-1]?.id || selected;
      if (completed) completed();
      $('status').textContent = 'Saved · Updated on your Portal'; showError('');
    } catch (error) { showError(error.message || 'Could not save. Try again.'); }
    finally { busy = false; document.querySelectorAll('button').forEach(button => button.disabled = false); render(); }
  }
  $('lists').addEventListener('change', () => { selected = $('lists').value; render(); });
  $('add-item').addEventListener('submit', event => {
    event.preventDefault(); const text = $('item').value.trim();
    if (text && selected) mutate({action:'addItem',listId:selected,text}, () => { $('item').value=''; $('item').focus(); });
  });
  $('add-list').addEventListener('submit', event => {
    event.preventDefault(); const name = $('name').value.trim();
    if (name) mutate({action:'addList',name}, () => { $('name').value=''; $('name').closest('details').open=false; });
  });
  document.addEventListener('visibilitychange', refresh);
  if (!token) { $('status').textContent = 'Link your phone'; showError('On the Portal, open Lists → Use from phone, then scan the QR code.'); }
  else { refresh(); setInterval(refresh, 4000); }
})();

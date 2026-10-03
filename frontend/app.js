'use strict';

// Test client for the Lingua API. Same origin by default (nginx forwards /api to the gateway);
// for local development against another host, open the page with ?api=http://localhost:8080
const API = new URLSearchParams(location.search).get('api') || '';

const DIFFICULTY_LABELS = { EASY: 'Facile', MEDIUM: 'Moyen', HARD: 'Difficile' };
const SOUND_LABELS = { SUCCESS: 'Son de réussite', FAILURE: "Son d'échec" };
// The server accepts an answer up to 2 s after the end of the timer: wait a little longer
// before asking for the score, so the expired attempt is settled
const SETTLE_DELAY_MS = 2600;

const state = {
    token: sessionStorage.getItem('token'),
    account: null,
    settings: null,
    languages: [],
    categories: [],
    attempt: null,
    answered: false,
    countdown: null,
    score: null,
};

const $ = (selector, root = document) => root.querySelector(selector);

/** Build an element. Text always goes through textContent: nothing from the API is parsed as HTML. */
function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
        if (key === 'class') node.className = value;
        else if (key === 'text') node.textContent = value;
        else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
        else if (value !== false && value != null) node.setAttribute(key, value === true ? '' : value);
    }
    for (const child of children) {
        if (child != null) node.append(child);
    }
    return node;
}

function isAdmin() {
    return !!state.account && state.account.authorities.includes('ROLE_ADMIN');
}

// ── API ────────────────────────────────────────────────────────

async function api(method, path, body) {
    const headers = {};
    if (state.token) headers.Authorization = 'Bearer ' + state.token;
    const options = { method, headers };
    if (body instanceof FormData) {
        options.body = body;
    } else if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(API + path, options);
    } catch (e) {
        throw new Error('Serveur injoignable');
    }
    if (response.status === 401 && state.token) {
        logout();
        throw new Error('Session expirée, reconnecte-toi');
    }
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = null;
        }
    }
    if (!response.ok) {
        throw new Error(describeError(response.status, data));
    }
    return { status: response.status, data, headers: response.headers };
}

function describeError(status, problem) {
    if (!problem) return 'Erreur ' + status;
    let message = problem.detail || problem.title || 'Erreur ' + status;
    if (Array.isArray(problem.fieldErrors) && problem.fieldErrors.length) {
        message += ' : ' + problem.fieldErrors.map(error => error.field + ' ' + error.message).join(', ');
    }
    return message;
}

function showMessage(text, kind) {
    const box = $('#message');
    box.className = kind;
    box.replaceChildren(el('div', { text }));
    box.hidden = false;
}

function clearMessage() {
    $('#message').hidden = true;
}

/** Run an action, showing its error if it fails. */
async function guard(action) {
    try {
        clearMessage();
        return await action();
    } catch (e) {
        showMessage(e.message, 'error');
        return undefined;
    }
}

// ── Session ────────────────────────────────────────────────────

async function login(username, password) {
    const { data } = await api('POST', '/api/authenticate', { username, password });
    state.token = data.id_token;
    sessionStorage.setItem('token', state.token);
    await startSession();
}

function logout() {
    stopCountdown();
    state.token = null;
    state.account = null;
    state.attempt = null;
    sessionStorage.removeItem('token');
    renderSession();
}

async function startSession() {
    state.account = (await api('GET', '/api/account')).data;
    await Promise.all([loadCatalog(), loadSettings(), loadScore()]);
    renderSession();
    showTab('play');
}

function renderSession() {
    const signedIn = !!state.account;
    $('#auth').hidden = signedIn;
    $('#tabs').hidden = !signedIn;
    for (const button of document.querySelectorAll('#tabs [data-admin]')) {
        button.hidden = !isAdmin();
    }
    if (!signedIn) {
        for (const section of document.querySelectorAll('main > section[id^="tab-"]')) section.hidden = true;
        $('#session').replaceChildren();
        return;
    }
    $('#session').replaceChildren(
        el('span', { text: state.account.login + (isAdmin() ? ' (admin)' : '') + ' ' }),
        el('button', { class: 'secondary', type: 'button', text: 'Déconnexion', onclick: logout })
    );
}

function showTab(name) {
    for (const button of document.querySelectorAll('#tabs button')) {
        button.classList.toggle('active', button.dataset.tab === name);
    }
    for (const section of document.querySelectorAll('main > section[id^="tab-"]')) {
        section.hidden = section.id !== 'tab-' + name;
    }
    clearMessage();
    const loaders = {
        leaderboard: loadLeaderboard,
        'admin-questions': loadQuestions,
        'admin-settings': renderDifficulties,
        'admin-sounds': renderSounds,
        'admin-catalog': renderCatalogAdmin,
    };
    if (loaders[name]) guard(loaders[name]);
}

// ── Shared data ────────────────────────────────────────────────

async function loadCatalog() {
    const [languages, categories] = await Promise.all([api('GET', '/api/languages'), api('GET', '/api/categories')]);
    state.languages = languages.data;
    state.categories = categories.data;

    fillSelect($('#play-category'), [['', 'Toutes']].concat(state.categories.map(category => [category.id, category.name])));
    fillSelect(
        $('#question-form [name=categoryId]'),
        state.categories.map(category => [category.id, category.name])
    );
    fillSelect(
        $('#category-form [name=languageId]'),
        state.languages.map(language => [language.id, language.name])
    );
}

function fillSelect(select, options) {
    const previous = select.value;
    select.replaceChildren(...options.map(([value, label]) => el('option', { value, text: label })));
    if (options.some(([value]) => String(value) === previous)) select.value = previous;
}

async function loadSettings() {
    state.settings = (await api('GET', '/api/settings/game')).data;
    $('#rules').textContent = state.settings.difficulties
        .map(d => `${DIFFICULTY_LABELS[d.difficulty]} : +${d.points} / −${d.penaltyPoints}, ${d.timeLimitSeconds} s`)
        .join('   ·   ');
}

async function loadScore() {
    state.score = (await api('GET', '/api/scores/me')).data;
    renderScore();
}

function renderScore() {
    if (state.score) $('#score').textContent = state.score.totalPoints + ' points';
}

// ── Game ───────────────────────────────────────────────────────

async function nextQuestion() {
    stopCountdown();
    const params = new URLSearchParams();
    if ($('#play-category').value) params.set('categoryId', $('#play-category').value);
    if ($('#play-difficulty').value) params.set('difficulty', $('#play-difficulty').value);

    const { status, data } = await api('POST', '/api/questions/next?' + params);
    // An attempt left unanswered may have been settled by this call
    await loadScore();
    if (status === 204) {
        state.attempt = null;
        $('#question').hidden = true;
        showMessage('Il ne reste aucune question à réussir dans cette sélection.', 'ok');
        return;
    }
    state.attempt = data;
    state.answered = false;
    renderQuestion();
    startCountdown(data.remainingSeconds, data.timeLimitSeconds);
}

function renderQuestion() {
    const { question } = state.attempt;
    $('#question').hidden = false;
    $('#question-level').textContent = DIFFICULTY_LABELS[question.difficulty];
    $('#question-text').textContent = question.text;
    $('#result').hidden = true;
    $('#choices').replaceChildren(
        ...question.choices.map(choice => {
            const content = choice.type === 'IMAGE' ? el('img', { src: choice.imageUrl, alt: 'Choix en image' }) : choice.text;
            return el('button', { type: 'button', 'data-choice': choice.id, onclick: () => guard(() => answer(choice.id)) }, content);
        })
    );
}

function startCountdown(remainingSeconds, timeLimitSeconds) {
    // Count from the remaining time given by the server, not from the clock of this device
    const deadline = performance.now() + remainingSeconds * 1000;
    const bar = $('#timer-bar');
    const tick = () => {
        const left = Math.max(0, deadline - performance.now());
        $('#timer-text').textContent = Math.ceil(left / 1000) + ' s';
        bar.style.width = (100 * left) / (timeLimitSeconds * 1000) + '%';
        bar.classList.toggle('low', left < 5000);
        if (left <= 0) {
            stopCountdown();
            guard(timeExpired);
        }
    };
    tick();
    state.countdown = setInterval(tick, 100);
}

function stopCountdown() {
    if (state.countdown) clearInterval(state.countdown);
    state.countdown = null;
}

function lockChoices() {
    for (const button of document.querySelectorAll('#choices button')) button.disabled = true;
}

async function answer(choiceId) {
    if (state.answered) return;
    state.answered = true;
    stopCountdown();
    lockChoices();
    let result;
    try {
        result = (await api('POST', `/api/attempts/${state.attempt.attemptId}/answer`, { choiceId })).data;
    } catch (e) {
        state.answered = false;
        throw e;
    }
    showResult(result, choiceId);
}

/** The timer reached zero without an answer: the server applies the penalty when the score is read. */
async function timeExpired() {
    if (state.answered) return;
    state.answered = true;
    lockChoices();
    const before = state.score ? state.score.totalPoints : 0;
    showOutcome('Temps écoulé…', false);
    await new Promise(resolve => setTimeout(resolve, SETTLE_DELAY_MS));
    await loadScore();
    showOutcome('Temps écoulé. ' + formatDelta(state.score.totalPoints - before), false);
    playSound(state.settings.failureSoundUrl);
}

function showResult(result, chosenId) {
    for (const button of document.querySelectorAll('#choices button')) {
        const id = Number(button.dataset.choice);
        if (id === result.correctChoiceId) button.classList.add('correct');
        else if (id === chosenId) button.classList.add('wrong');
    }
    const labels = { CORRECT: 'Bonne réponse !', WRONG: 'Mauvaise réponse.', TIME_EXPIRED: 'Temps écoulé.' };
    showOutcome(labels[result.outcome] + ' ' + formatDelta(result.pointsDelta), result.outcome === 'CORRECT');
    state.score = { ...state.score, totalPoints: result.totalPoints };
    renderScore();
    playSound(result.soundUrl);
}

function showOutcome(text, good) {
    const result = $('#result');
    result.textContent = text;
    result.className = good ? 'good' : 'bad';
    result.hidden = false;
}

function formatDelta(delta) {
    if (delta > 0) return '+' + delta + ' points';
    if (delta < 0) return '−' + -delta + ' points';
    return 'Aucun point gagné ni perdu.';
}

function playSound(url) {
    if (!url) return;
    // Browsers may refuse to play without a recent click: the game must go on anyway
    new Audio(url).play().catch(() => {});
}

async function loadLeaderboard() {
    const { data } = await api('GET', '/api/scores/leaderboard?size=20');
    $('#leaderboard').replaceChildren(
        ...data.map((score, index) =>
            el(
                'tr',
                {},
                el('td', { text: index + 1 }),
                el('td', { text: score.userLogin }),
                el('td', { text: score.totalPoints }),
                el('td', { text: score.correctAnswers })
            )
        )
    );
}

// ── Admin: questions ───────────────────────────────────────────

function addChoiceRow() {
    const text = el('input', { type: 'text', placeholder: 'Texte du choix', maxlength: 500 });
    const file = el('input', { type: 'file', accept: 'image/*', hidden: true });
    const preview = el('img', { alt: '', hidden: true });
    const type = el(
        'select',
        {
            onchange: () => {
                const image = type.value === 'IMAGE';
                text.hidden = image;
                file.hidden = !image;
                preview.hidden = !image || !file.files.length;
            },
        },
        el('option', { value: 'TEXT', text: 'Texte' }),
        el('option', { value: 'IMAGE', text: 'Image' })
    );
    file.addEventListener('change', () => {
        if (preview.src) URL.revokeObjectURL(preview.src);
        preview.hidden = !file.files.length;
        if (file.files.length) preview.src = URL.createObjectURL(file.files[0]);
    });
    const row = el(
        'div',
        { class: 'choice-row' },
        el('input', { type: 'radio', name: 'correct', title: 'Bon choix' }),
        type,
        text,
        file,
        preview,
        el('button', { type: 'button', class: 'danger', text: 'Retirer', onclick: () => row.remove() })
    );
    $('#choice-rows').append(row);
}

function resetQuestionForm() {
    $('#question-form').reset();
    $('#choice-rows').replaceChildren();
    addChoiceRow();
    addChoiceRow();
}

async function uploadImage(file) {
    const form = new FormData();
    form.append('files', file);
    return (await api('POST', '/api/media/images/upload', form)).data[0].url;
}

async function saveQuestion(form) {
    const rows = [...document.querySelectorAll('#choice-rows .choice-row')];
    if (rows.some(row => row.querySelector('select').value === 'IMAGE' && !row.querySelector('input[type=file]').files.length)) {
        throw new Error('Choisis un fichier pour chaque choix de type Image');
    }
    const choices = [];
    for (const row of rows) {
        const type = row.querySelector('select').value;
        const correct = row.querySelector('input[type=radio]').checked;
        if (type === 'IMAGE') {
            choices.push({ type, imageUrl: await uploadImage(row.querySelector('input[type=file]').files[0]), correct });
        } else {
            choices.push({ type, text: row.querySelector('input[type=text]').value, correct });
        }
    }
    await api('POST', '/api/admin/questions', {
        text: form.text.value,
        difficulty: form.difficulty.value,
        categoryId: Number(form.categoryId.value),
        choices,
    });
    resetQuestionForm();
    showMessage('Question enregistrée.', 'ok');
    await loadQuestions();
}

async function loadQuestions() {
    const { data, headers } = await api('GET', '/api/admin/questions?size=100');
    $('#question-count').textContent = '(' + (headers.get('X-Total-Count') || data.length) + ')';
    const categoryNames = new Map(state.categories.map(category => [category.id, category.name]));
    $('#question-list').replaceChildren(
        ...data.map(question =>
            el(
                'div',
                { class: 'question-item' },
                el(
                    'div',
                    { class: 'row' },
                    el('strong', { text: question.text }),
                    el('span', { class: 'badge', text: DIFFICULTY_LABELS[question.difficulty] }),
                    el('span', { class: 'badge', text: categoryNames.get(question.categoryId) || '?' }),
                    el('span', { class: 'spacer' }),
                    el('button', {
                        type: 'button',
                        class: 'danger',
                        text: 'Supprimer',
                        onclick: () =>
                            guard(async () => {
                                if (!confirm('Supprimer cette question ?')) return;
                                await api('DELETE', '/api/admin/questions/' + question.id);
                                await loadQuestions();
                            }),
                    })
                ),
                el(
                    'ul',
                    {},
                    ...question.choices.map(choice =>
                        el(
                            'li',
                            { class: choice.correct ? 'correct' : '' },
                            choice.type === 'IMAGE' ? el('img', { src: choice.imageUrl, alt: 'Choix en image' }) : choice.text,
                            choice.correct ? ' ✓' : null
                        )
                    )
                )
            )
        )
    );
}

// ── Admin: settings ────────────────────────────────────────────

async function renderDifficulties() {
    const { data } = await api('GET', '/api/admin/settings/difficulties');
    $('#difficulties').replaceChildren(
        ...data.map(setting => {
            const points = el('input', { type: 'number', min: 0, max: 10000, value: setting.points });
            const penalty = el('input', { type: 'number', min: 0, max: 10000, value: setting.penaltyPoints });
            const time = el('input', { type: 'number', min: 5, max: 600, value: setting.timeLimitSeconds });
            const save = () =>
                guard(async () => {
                    await api('PUT', '/api/admin/settings/difficulties/' + setting.difficulty, {
                        points: Number(points.value),
                        penaltyPoints: Number(penalty.value),
                        timeLimitSeconds: Number(time.value),
                    });
                    await loadSettings();
                    showMessage('Niveau ' + DIFFICULTY_LABELS[setting.difficulty] + ' enregistré.', 'ok');
                });
            return el(
                'tr',
                {},
                el('td', { text: DIFFICULTY_LABELS[setting.difficulty] }),
                el('td', {}, points),
                el('td', {}, penalty),
                el('td', {}, time),
                el('td', {}, el('button', { type: 'button', text: 'Enregistrer', onclick: save }))
            );
        })
    );
}

// ── Admin: sounds ──────────────────────────────────────────────

async function renderSounds() {
    const { data } = await api('GET', '/api/admin/settings/sounds');
    const urls = new Map(data.map(sound => [sound.event, sound.audioUrl]));
    $('#sounds').replaceChildren(
        ...Object.keys(SOUND_LABELS).map(event => {
            const url = urls.get(event);
            const file = el('input', { type: 'file', accept: 'audio/*,.mp3,.wav,.ogg' });
            const upload = () =>
                guard(async () => {
                    if (!file.files.length) throw new Error('Choisis un fichier audio');
                    const form = new FormData();
                    form.append('file', file.files[0]);
                    const uploaded = (await api('POST', '/api/media/audio/upload', form)).data;
                    await api('PUT', '/api/admin/settings/sounds/' + event, { audioUrl: uploaded.url });
                    await Promise.all([loadSettings(), renderSounds()]);
                    showMessage(SOUND_LABELS[event] + ' enregistré.', 'ok');
                });
            const remove = () =>
                guard(async () => {
                    await api('DELETE', '/api/admin/settings/sounds/' + event);
                    await Promise.all([loadSettings(), renderSounds()]);
                });
            return el(
                'div',
                { class: 'card' },
                el('h2', { text: SOUND_LABELS[event] }),
                url ? el('audio', { controls: true, src: url }) : el('p', { class: 'muted', text: 'Aucun son configuré.' }),
                el(
                    'div',
                    { class: 'row' },
                    file,
                    el('button', { type: 'button', text: 'Envoyer', onclick: upload }),
                    url ? el('button', { type: 'button', class: 'danger', text: 'Retirer', onclick: remove }) : null
                )
            );
        })
    );
}

// ── Admin: catalog ─────────────────────────────────────────────

async function renderCatalogAdmin() {
    await loadCatalog();
    $('#category-list').replaceChildren(
        ...state.categories.map(category =>
            el(
                'tr',
                {},
                el('td', { text: category.languageCode }),
                el('td', { text: category.name }),
                el('td', { text: category.description || '' }),
                el(
                    'td',
                    {},
                    el('button', {
                        type: 'button',
                        class: 'danger',
                        text: 'Supprimer',
                        onclick: () =>
                            guard(async () => {
                                if (!confirm('Supprimer la catégorie ' + category.name + ' ?')) return;
                                await api('DELETE', '/api/admin/categories/' + category.id);
                                await renderCatalogAdmin();
                            }),
                    })
                )
            )
        )
    );
}

// ── Wiring ─────────────────────────────────────────────────────

$('#login-form').addEventListener('submit', event => {
    event.preventDefault();
    const form = event.target;
    guard(() => login(form.username.value, form.password.value));
});

$('#register-form').addEventListener('submit', event => {
    event.preventDefault();
    const form = event.target;
    guard(async () => {
        await api('POST', '/api/register', { login: form.login.value, email: form.email.value, password: form.password.value });
        await login(form.login.value, form.password.value);
    });
});

$('#tabs').addEventListener('click', event => {
    if (event.target.dataset.tab) showTab(event.target.dataset.tab);
});

$('#play-next').addEventListener('click', () => guard(nextQuestion));
$('#add-choice').addEventListener('click', addChoiceRow);

$('#question-form').addEventListener('submit', event => {
    event.preventDefault();
    guard(() => saveQuestion(event.target));
});

$('#category-form').addEventListener('submit', event => {
    event.preventDefault();
    const form = event.target;
    guard(async () => {
        await api('POST', '/api/admin/categories', {
            name: form.name.value,
            description: form.description.value || null,
            languageId: Number(form.languageId.value),
        });
        form.reset();
        await renderCatalogAdmin();
    });
});

resetQuestionForm();
renderSession();
if (state.token) {
    guard(startSession);
}

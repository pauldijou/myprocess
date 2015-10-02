import { createStore, combineReducers, compose } from 'redux';
import { devTools, persistState } from 'redux-devtools';
import { bindAll } from 'redux-act';

import loginReducer from './login/reducer';
import * as loginActions from './login/actions';

const finalCreateStore = compose(
  devTools(),
  persistState(window.location.href.match(/[?&]debug_session=([^&])\b/)),
)(createStore);

const store = finalCreateStore(combineReducers({
  user: loginReducer
}));

bindAll(loginActions, store);

// FIXME: only expose on dev mode
window.store = store;

export default store;

import { createReducer } from 'redux-act';
import * as actions from './actions';
import JWT from 'jwt-client';

export default createReducer({
  [actions.login]: (state, user)=> {
    JWT.keep(user);
    return user;
  },
  [actions.logout]: ()=> {
    JWT.forget();
    return false;
  }
}, JWT.remember() || false);

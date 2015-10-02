import React from 'react';
import grab from 'grab-http';
import JWT from 'jwt-client';
import { login, logout } from '../login/actions';

export default class Main extends React.Component {
  login = ()=> {
    grab.post('/api/v1/login', { email: this.refs.email.value, password: this.refs.password.value})
      .then(response => {
        console.log('status', response.status);
        console.log('header', response.headers['authorization']);
        const user = JWT.read(response.headers['authorization']);
        console.log('user', user);

        if (JWT.validate(user)) {
          login(user);
        } else {
          logout();
        }
      })
      .catch(error => {
        console.log(error);
        logout();
      });
  }

  render () {
    return (<div>
      <input type="text" ref="email" />
      <input type="text" ref="password" />
      <button type="button" onClick={this.login}>Login</button>
    </div>);
  }
}

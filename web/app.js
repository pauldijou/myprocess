import styles from './app.scss';

import polyfills from './polyfills';
import React from 'react';
import ReactDom from 'react-dom';
import { Provider } from 'react-redux';
import { DevTools, DebugPanel, LogMonitor } from 'redux-devtools/lib/react';
import { Router, Route, IndexRoute } from 'react-router';
import createBrowserHistory from 'react-router/node_modules/history/lib/createBrowserHistory';

import store from './store';

import Main from './main';
import Home from './home';

ReactDom.render((<div>
  <Provider store={store}>
    <Router history={createBrowserHistory()}>
      <Route component={Main}>
        <Route path='/' component={Home} />
      </Route>
    </Router>
  </Provider>
  <DebugPanel top right bottom>
    <DevTools store={store} monitor={LogMonitor} visibleOnLoad={false} />
  </DebugPanel>
</div>), document.getElementById('main'));

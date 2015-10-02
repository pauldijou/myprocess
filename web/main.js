import React from 'react';

export default class Main extends React.Component {
  render () {
    return (<div>
      <div>My Process</div>
      {this.props.children}
    </div>);
  }
}

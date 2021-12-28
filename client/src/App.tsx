import { Main } from './components/Main';


function App() {
  return (
    <div className="App">
      <Main {... { proxyAddr: { port: "8888" }, replAddr: { port: "7777", type: "prepl" } }} />
    </div>
  );
}

export default App;

import { Layout, Menu, message } from "antd";
import React, { useDeferredValue, useEffect, useState } from "react";
import {
  logout,
  getTopGames,
  getRecommendations,
  searchGameById,
  getFavoriteItem,
} from "./utils";
import CustomSearch from "./components/CustomSearch";
import { LikeOutlined, FireOutlined } from "@ant-design/icons";
import SubMenu from "antd/lib/menu/SubMenu";
import TopGames from "./components/TopGames";
import PageHeader from "./components/PageHeader";
import Home from "./components/Home";
 
const { Sider, Content } = Layout;
 
function App() {
  const [loggedIn, setLoggedIn] = useState(false);
  const [topGames, setTopGames] = useState([]);
  const [resources, setResources] = useState({
    VIDEO: [],
    STREAM: [],
    CLIP: [],
  });
  const [favoriteItems, setFavoriteItems] = useState({
    VIDEO: [],
    STREAM: [],
    CLIP: [],
  });
 
  const signinOnSuccess = () => {
    getFavoriteItem().then((data) => {
      setLoggedIn(true);
      setFavoriteItems(data);
    });
  };
 
  const signoutOnClick = () => {
    logout()
      .then(() => {
        setLoggedIn(false);
        message.success("Successfully Signed out");
      })
      .catch((err) => {
        message.error(err.message);
      });
  };
 
  const onGameSelect = ({ key }) => {
    if (key === "Recommendation") {
      getRecommendations().then((data) => {
        setResources(data);
      });
 
      return;
    }
 
    searchGameById(key).then((data) => {
      setResources(data);
    });
  };
 
  const customSearchOnSuccess = (data) => {
    setResources(data);
  };
 
  const favoriteOnChange = () => {
    getFavoriteItem()
      .then((data) => {
        setFavoriteItems(data);
      })
      .catch((err) => {
        message.error(err.message);
      });
  };
 
  useEffect(() => {
    getTopGames()
      .then((data) => {
        setTopGames(data);
      })
      .catch((err) => {
        message.error(err.message);
      });
  }, [getTopGames]);
 
  return (
    <Layout>
      <PageHeader
        loggedIn={loggedIn}
        signoutOnClick={signoutOnClick}
        signinOnSuccess={signinOnSuccess}
        data={favoriteItems}
      />
      <Layout>
        <Sider width={300} className="site-layout-background">
          <CustomSearch onSuccess={customSearchOnSuccess} />
          <Menu
            mode="inline"
            onSelect={onGameSelect}
            style={{ marginTop: "10px" }}
          >
            <Menu.Item icon={<LikeOutlined />} key="Recommendation">
              Recommend for you!
            </Menu.Item>
            <SubMenu
              icon={<FireOutlined />}
              key="Popular Games"
              title="Popular Games"
              className="site-top-game-list"
            >
              {/* <TopGames topGames={topGames} /> */}
              {TopGames({ topGames })}
            </SubMenu>
          </Menu>
        </Sider>
        <Layout style={{ padding: "24px" }}>
          <Content
            className="site-layout-background"
            style={{
              padding: 24,
              margin: 0,
              height: 800,
              overflow: "auto",
            }}
          >
            <Home
              favoriteOnChange={favoriteOnChange}
              resources={resources}
              favoriteItems={favoriteItems}
              loggedIn={loggedIn}
            />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
}
 
export default App;
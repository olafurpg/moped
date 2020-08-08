// See https://docusaurus.io/docs/site-config.html for all the possible
// site configuration options.

const repoUrl = "https://github.com/scalameta/moped";
const baseUrl = "/moped/";
const title = "Moped 🛵";
const tagline = "Scala library to build command-line applications";
const url = "https://scalameta.org/moped";
const features = [
  {
    title: "Automatic",
    content:
      "Declare your command-line application as a Scala case class and let Moped automatically generate the command-line parser and `--help` message.",
    image: "https://i.imgur.com/w7YzxOU.png",
    imageAlign: "left",
  },
  {
    title: "Configurable",
    content:
      "Allow your users to declare configuration in HOCON and JSON files so they don't have to repeat the same command-line flags on every invocation.",
    image: "https://i.imgur.com/zKu8dz4.png",
    imageAlign: "right",
  },
  {
    title: "Testable",
    content:
      "Use Moped testkit to easily test your command-line application's interactions with the file system and standard output.",
    image: "https://i.imgur.com/oXQuFal.png",
    imageAlign: "left",
  },
];

const siteConfig = {
  title: title,
  tagline: tagline,
  url: url,
  baseUrl: baseUrl,

  // Used for publishing and more
  projectName: "moped",
  organizationName: "scalameta",

  // algolia: {
  //   apiKey: "586dbbac9432319747bfea750fab16cb",
  //   indexName: "scalameta_munit"
  // },

  gaTrackingId: "UA-140140828-1",

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    { doc: "getting-started", label: "Docs" },
    { blog: true, label: "Blog" },
    { href: repoUrl, label: "GitHub", external: true },
  ],

  // If you have users set above, you add it here:
  // users,

  /* path to images for header/footer */
  headerIcon: "img/scalameta-logo.png",
  footerIcon: "img/scalameta-logo.png",
  favicon: "img/favicon.ico",

  /* colors for website */
  colors: {
    primaryColor: "#440069",
    secondaryColor: "#290040",
  },

  customDocsPath: "website/target/docs",

  stylesheets: [baseUrl + "css/custom.css"],

  blogSidebarCount: "ALL",

  // This copyright info is used in /core/Footer.js and blog rss/atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} Scalameta`,

  usePrism: ["scala"],

  highlight: {
    theme: "github",
  },

  /* On page navigation for the current documentation page */
  onPageNav: "separate",

  /* Open Graph and Twitter card images */
  ogImage: "img/scalameta-logo.png",
  twitterImage: "img/scalameta-logo.png",

  editUrl: `${repoUrl}/edit/master/docs/`,

  repoUrl,
  features: features,
};

module.exports = siteConfig;

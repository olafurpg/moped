// See https://docusaurus.io/docs/site-config.html for all the possible
// site configuration options.

const repoUrl = "https://github.com/scalameta/moped";
const baseUrl = "/moped/";
const title = "Moped";
const tagline = "Scala library to buildcommand-line applications";
const url = "https://scalameta.org/moped";
const features = [
  {
    title: "HOCON and JSON",
    content:
      "Use Moped to convert HOCON and JSON configuration into Scala case classes.",
    image: "https://i.imgur.com/zKu8dz4.png",
    imageAlign: "left",
  },
  {
    title: "Command-line parsing",
    content:
      "Use Moped to build command-line tools with automatic `--help` message generation, clear error reporting and tab completion support. " +
      "Command-line flags are converted into Scala case classes, just like HOCON and JSON configuration.",
    image: "https://i.imgur.com/w7YzxOU.png",
    imageAlign: "right",
  },
  {
    title: "Documentation generation",
    content:
      "Automatically generate markdown documentation for all configuration options in your application, " +
      "including example usages, deprecation notices, command-line flags, and more.",
    image: "https://i.imgur.com/zWCMmhu.png",
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

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks
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

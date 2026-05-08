import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, BatchWriteCommand } from "@aws-sdk/lib-dynamodb";

const client = new DynamoDBClient({ region: "eu-central-1" });
const docClient = DynamoDBDocumentClient.from(client);

const products = [
  { id: "1",  title: "Apple MacBook Pro 14\"",           description: "M3 Pro chip, 18GB RAM, 512GB SSD, Space Black",         price: 1999 },
  { id: "2",  title: "Sony WH-1000XM5",                  description: "Wireless noise-cancelling over-ear headphones, black",  price: 349  },
  { id: "3",  title: "Samsung 65\" QLED 4K TV",          description: "QN65Q80C, 120Hz, Quantum HDR, Smart TV 2023",           price: 1197 },
  { id: "4",  title: "Apple AirPods Pro (2nd Gen)",       description: "Active noise cancellation, MagSafe USB-C case",        price: 249  },
  { id: "5",  title: "Logitech MX Master 3S",            description: "Wireless ergonomic mouse, 8K DPI, silent clicks",       price: 99   },
  { id: "6",  title: "Dell UltraSharp 27\" 4K Monitor",  description: "U2723QE, IPS Black, USB-C 90W, color accurate",        price: 579  },
  { id: "7",  title: "Kindle Paperwhite (11th Gen)",     description: "6.8\" display, 32GB, waterproof, adjustable warm light",price: 139  },
  { id: "8",  title: "GoPro HERO12 Black",               description: "5.3K60 video, HyperSmooth 6.0, waterproof to 10m",     price: 399  },
  { id: "9",  title: "DJI Mini 4 Pro",                   description: "4K/60fps drone, 34-min flight, omnidirectional sensing",price: 759  },
  { id: "10", title: "Apple iPad Pro 12.9\" M2",         description: "256GB Wi-Fi, Liquid Retina XDR, Apple Pencil support",  price: 1099 },
  { id: "11", title: "Dyson V15 Detect",                 description: "Cordless vacuum, laser dust detection, HEPA filtration",price: 749  },
  { id: "12", title: "Bose QuietComfort 45",             description: "Bluetooth headphones, 24h battery, noise cancelling",  price: 279  },
  { id: "13", title: "Samsung Galaxy Tab S9",            description: "11\" AMOLED, Snapdragon 8 Gen 2, 128GB, IP68",          price: 799  },
  { id: "14", title: "Anker 737 Power Bank",             description: "24,000mAh, 140W USB-C, bi-directional fast charging",  price: 109  },
  { id: "15", title: "Razer BlackWidow V4 Pro",          description: "Wireless mechanical keyboard, Razer Yellow switches",  price: 229  },
  { id: "16", title: "LG C3 55\" OLED 4K TV",           description: "Evo panel, 120Hz, G-Sync, Dolby Vision & Atmos",        price: 1296 },
  { id: "17", title: "Sony PlayStation 5 Slim",          description: "Digital edition, 1TB SSD, DualSense controller",       price: 449  },
  { id: "18", title: "Elgato Stream Deck MK.2",          description: "15 LCD keys, USB-C, customisable actions, white",      price: 149  },
  { id: "19", title: "Rode NT-USB Mini",                 description: "Compact USB condenser microphone, studio quality",     price: 99   },
  { id: "20", title: "Apple Watch Series 9 45mm",        description: "GPS, aluminium, starlight, double-tap gesture, S9 chip",price: 429  },
  { id: "21", title: "Keychron Q1 Pro",                  description: "75% wireless mechanical keyboard, QMK/VIA, gasket",    price: 199  },
  { id: "22", title: "Secretlab TITAN Evo 2022",         description: "Gaming chair, SoftWeave fabric, lumbar support, L",    price: 549  },
  { id: "23", title: "Philips Hue Starter Kit (4 bulbs)","description": "E27 colour smart bulbs, Hue Bridge, works with Alexa",price: 179  },
  { id: "24", title: "Canon EOS R50",                    description: "24.2MP APS-C mirrorless, 4K video, dual-pixel CMOS AF",price: 679  },
  { id: "25", title: "SteelSeries Arctis Nova Pro",      description: "Wireless gaming headset, active noise cancellation",   price: 349  },
];

const stocks = [
  { product_id: "1",  count: 5  },
  { product_id: "2",  count: 12 },
  { product_id: "3",  count: 3  },
  { product_id: "4",  count: 20 },
  { product_id: "5",  count: 35 },
  { product_id: "6",  count: 7  },
  { product_id: "7",  count: 18 },
  { product_id: "8",  count: 9  },
  { product_id: "9",  count: 4  },
  { product_id: "10", count: 6  },
  { product_id: "11", count: 8  },
  { product_id: "12", count: 14 },
  { product_id: "13", count: 11 },
  { product_id: "14", count: 25 },
  { product_id: "15", count: 10 },
  { product_id: "16", count: 2  },
  { product_id: "17", count: 15 },
  { product_id: "18", count: 22 },
  { product_id: "19", count: 30 },
  { product_id: "20", count: 8  },
  { product_id: "21", count: 13 },
  { product_id: "22", count: 1  },
  { product_id: "23", count: 17 },
  { product_id: "24", count: 6  },
  { product_id: "25", count: 9  },
];

async function fillTable(tableName, items) {
  const requests = items.map((item) => ({ PutRequest: { Item: item } }));
  await docClient.send(new BatchWriteCommand({ RequestItems: { [tableName]: requests } }));
  console.log(`Filled table "${tableName}" with ${items.length} items.`);
}

await fillTable("products", products);
await fillTable("stocks", stocks);

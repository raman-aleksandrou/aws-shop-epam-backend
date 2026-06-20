import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import {
  DynamoDBDocumentClient,
  BatchWriteCommand,
  ScanCommand,
} from "@aws-sdk/lib-dynamodb";

const client = new DynamoDBClient({ region: "eu-central-1" });
const docClient = DynamoDBDocumentClient.from(client);

const products = [
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0801", title: "Apple MacBook Pro 14\"",           description: "M3 Pro chip, 18GB RAM, 512GB SSD, Space Black",         price: 1999 },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0802", title: "Sony WH-1000XM5",                  description: "Wireless noise-cancelling over-ear headphones, black",  price: 349  },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0803", title: "Samsung 65\" QLED 4K TV",          description: "QN65Q80C, 120Hz, Quantum HDR, Smart TV 2023",           price: 1197 },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0804", title: "Apple AirPods Pro (2nd Gen)",       description: "Active noise cancellation, MagSafe USB-C case",        price: 249  },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0805", title: "Logitech MX Master 3S",            description: "Wireless ergonomic mouse, 8K DPI, silent clicks",       price: 99   },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0806", title: "Dell UltraSharp 27\" 4K Monitor",  description: "U2723QE, IPS Black, USB-C 90W, color accurate",        price: 579  },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0807", title: "Kindle Paperwhite (11th Gen)",     description: "6.8\" display, 32GB, waterproof, adjustable warm light",price: 139  },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0808", title: "GoPro HERO12 Black",               description: "5.3K60 video, HyperSmooth 6.0, waterproof to 10m",     price: 399  },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0809", title: "DJI Mini 4 Pro",                   description: "4K/60fps drone, 34-min flight, omnidirectional sensing",price: 759  },
  { id: "d290f1ee-6c54-4b01-90e6-d701748f0810", title: "Apple iPad Pro 12.9\" M2",         description: "256GB Wi-Fi, Liquid Retina XDR, Apple Pencil support",  price: 1099 },
];

const stocks = [
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0801", count: 5  },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0802", count: 12 },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0803", count: 3  },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0804", count: 20 },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0805", count: 35 },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0806", count: 7  },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0807", count: 18 },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0808", count: 9  },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0809", count: 4  },
  { product_id: "d290f1ee-6c54-4b01-90e6-d701748f0810", count: 6  },
];

// Image links are stored on the BE side (in the Products table), not generated on the client.
// Real product photos (Wikimedia Commons) matching each product's title and description.
const productImages = {
  "d290f1ee-6c54-4b01-90e6-d701748f0801": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/MacBook_Pro_16_%28M1_Pro%2C_2021%29_-_Wikipedia.jpg/330px-MacBook_Pro_16_%28M1_Pro%2C_2021%29_-_Wikipedia.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0802": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.jpg/500px-Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0803": "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7c/Samsung_QLED_TV_8K_-_75_inches_-_2018-11-02.jpg/500px-Samsung_QLED_TV_8K_-_75_inches_-_2018-11-02.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0804": "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/AirPods_Pro_3_with_case.jpg/330px-AirPods_Pro_3_with_case.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0805": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Logitech_MX_Master_3S_HS12.jpg/500px-Logitech_MX_Master_3S_HS12.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0806": "https://upload.wikimedia.org/wikipedia/commons/thumb/7/73/Monitor_-_Flickr_-_davispuh.jpg/500px-Monitor_-_Flickr_-_davispuh.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0807": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/45/2023_Amazon_Kindle_Paperwhite_%282%29.jpg/500px-2023_Amazon_Kindle_Paperwhite_%282%29.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0808": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/40/GoPro_Hero_%288009036215%29.jpg/500px-GoPro_Hero_%288009036215%29.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0809": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/2024_Dron_DJI_Mini_4_Pro_%2818%29.jpg/330px-2024_Dron_DJI_Mini_4_Pro_%2818%29.jpg",
  "d290f1ee-6c54-4b01-90e6-d701748f0810": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Wikipedia_on_iPad_Pro.jpg/330px-Wikipedia_on_iPad_Pro.jpg",
};

const productsWithImages = products.map((p) => ({
  ...p,
  image: productImages[p.id],
}));

// DynamoDB BatchWrite accepts at most 25 requests per call.
function chunk(items, size = 25) {
  const chunks = [];
  for (let i = 0; i < items.length; i += size) {
    chunks.push(items.slice(i, i + size));
  }
  return chunks;
}

// Remove every existing item so the table ends up with exactly the seeded set.
async function clearTable(tableName, keyName) {
  let deleted = 0;
  let lastEvaluatedKey;
  do {
    const scan = await docClient.send(
      new ScanCommand({
        TableName: tableName,
        ProjectionExpression: keyName,
        ExclusiveStartKey: lastEvaluatedKey,
      })
    );
    const items = scan.Items ?? [];
    for (const group of chunk(items)) {
      const requests = group.map((item) => ({
        DeleteRequest: { Key: { [keyName]: item[keyName] } },
      }));
      await docClient.send(
        new BatchWriteCommand({ RequestItems: { [tableName]: requests } })
      );
      deleted += requests.length;
    }
    lastEvaluatedKey = scan.LastEvaluatedKey;
  } while (lastEvaluatedKey);
  console.log(`Cleared table "${tableName}" (${deleted} items removed).`);
}

async function fillTable(tableName, items) {
  for (const group of chunk(items)) {
    const requests = group.map((item) => ({ PutRequest: { Item: item } }));
    await docClient.send(
      new BatchWriteCommand({ RequestItems: { [tableName]: requests } })
    );
  }
  console.log(`Filled table "${tableName}" with ${items.length} items.`);
}

await clearTable("products", "id");
await clearTable("stocks", "product_id");

await fillTable("products", productsWithImages);
await fillTable("stocks", stocks);

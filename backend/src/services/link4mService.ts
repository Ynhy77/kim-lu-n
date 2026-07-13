import dotenv from 'dotenv';
dotenv.config();

export class Link4mService {
  /**
   * Shorten / Monetize a URL using Link4m
   * Endpoint: GET https://link4m.com/api-shortener/v2?api=API_KEY&url=URL
   */
  public static async shortenUrl(destinationUrl: string): Promise<string> {
    const apiKey = process.env.LINK4M_API_KEY;

    if (!apiKey || apiKey === 'THAY_BANG_KEY_MOI' || apiKey === 'MY_LINK4M_KEY') {
      console.warn('LINK4M_API_KEY is not configured or is a placeholder. Returning direct destination URL.');
      return destinationUrl;
    }

    try {
      const encodeDest = encodeURIComponent(destinationUrl);
      const requestUrl = `https://link4m.com/api-shortener/v2?api=${apiKey}&url=${encodeDest}`;

      console.log(`Sending request to Link4m API for URL: ${destinationUrl}`);
      const res = await fetch(requestUrl);
      
      if (!res.ok) {
        throw new Error(`Link4m API returned status ${res.status}`);
      }

      const json = await res.json() as any;
      
      // Link4m typical response structure:
      // { "status": "success", "shortenedUrl": "https://link4m.co/XXXX" } or similar
      if (json && json.status === 'success' && json.shortenedUrl) {
        return json.shortenedUrl;
      } else if (json && json.shortenedUrl) {
        return json.shortenedUrl;
      } else {
        console.error('Unexpected Link4m response structure:', json);
        return destinationUrl; // Fallback
      }
    } catch (e) {
      console.error('Error connecting to Link4m shortener API, returning direct destination URL.', e);
      return destinationUrl;
    }
  }
}

import {createCipheriv, randomBytes, randomInt} from 'crypto';

const makeNonce = () => randomInt(-2147483648, 2147483647);
const makeRandomIv = () => randomBytes(8).toString('hex');

export function generateToken04(appId, userId, secret, effectiveTimeInSeconds, payload){
    if(!appId || !userId || secret.length !== 32) throw new Error('Invalid params');
    
    const createTime = Math.floor(Date.now()/1000);
    const expire = createTime + effectiveTimeInSeconds;
    
    const tokenInfo = JSON.stringify({
        app_id: appId,
        user_id: userId,
        nonce: makeNonce(),
        ctime: createTime,
        expire: expire,
        payload: payload || ''
    });
    
    const iv = makeRandomIv();
    const cipher = createCipheriv('aes-256-cbc', Buffer.from(secret), Buffer.from(iv));
    const encryptBuf = Buffer.concat([cipher.update(tokenInfo), cipher.final()]);
    
    const pack = Buffer.alloc(28 + encryptBuf.length);
    pack.writeBigInt64BE(BigInt(expire), 0);
    pack.writeUInt16BE(iv.length, 8);
    pack.write(iv, 10);
    pack.writeUInt16BE(encryptBuf.length, 26);
    pack.set(encryptBuf, 28);
    
    return '04' + pack.toString('base64');
}
const { logger, mount } = require('paperplane')
const http = require('http')
const port = process.env.PORT || 3001
// Error.stackTraceLimit = Infinity

const parse = function parse(data) {
    // remove //isc_RPCResponseStart--> from start and end of string
    const trim = data.slice(25, data.length - 20);
    const clean = trim
	  .replace(/Date\.parseServerDate\((\d*,\d*,\d*)\)/g, (match, group1) => {
	      const [year, month, day] = group1.split(/\D/);
	      const string = new Date(year, month, day).toISOString();
	      return `"${string}"`;
	  })
	  .replace(/new Date\((-?\d*)\)/g, '$1');
    return eval(clean);
    // try {
    // 	return eval(clean);
    // } catch (e) {
    // 	console.log('DATA failling parse:\n', data.slice(0, 100));
    // 	console.log('Eval function error:\n', e);
    // 	return new Error('unquoted JSON parse failled', e);
    // }
 }

// const app = mount({
//   app: req => ({
//       body: JSON.stringify(parse(req.body)),
//       headers : {},
//       statusCode: 200
//   }),
//   logger
// })

// const app = req => ({
//   body: null,
//   headers: {},
//   statusCode: 200
// })

// http.createServer(mount({ app })).listen(3000)

const app = (req, res) => {

    let data = '';
    req.on('data', chunk => {
	data += chunk;
    })
    req.on('end', () => {
	let resp ;
	try {
	    resp = JSON.stringify(parse(data));
	} catch (e) {
	    console.log('DATA failling parse:\n', data.slice(0, 100));
	    console.log('Eval function error:\n', e);
	    res.statusCode = 500;
	    res.end(JSON.stringify({
		message: e }));

	}

	res.statusCode = 200
	res.end(resp)

    })
}

http.createServer(app).listen(port)

// const listening = err =>
//   err
//     ? console.error(err)
//     : console.info(`Listening on port: ${port}`)

// http.createServer(app).listen(port)

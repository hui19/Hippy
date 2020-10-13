function ccc() {
	console.log('ccc');
	global.dynamicLoad('../ccc.js');
}


console.log('========================xxxxxxxxxxxxxxxxxx');
ccc();
global.ccc = ccc;

global.abc = {
	a: 1,
	b: 2,
	c: 3,
	d: ccc
}

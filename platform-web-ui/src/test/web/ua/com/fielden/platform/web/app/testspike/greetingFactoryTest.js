define(['angular', 'angular.mocks', 
	'app/testspike/greetingFactory'], function(angular, angularMocks, greetingFactory) {
    
    describe('Greeting factory suite', function () {

    	var greeterService;
    	beforeEach(module('greeter'));
    	beforeEach(inject(function(_greeterService_) {
    		greeterService = _greeterService_;
    	}));
    
        it("Greeter service must return 'Hello, World' for 'World' parameter.", function() {
            expect(greeterService('World')).toBe('Hello, World');
        });

        it("Greeter must return 'Hello, Jhou' for 'Jhou' parameter.", function() {
            expect(greeterService('Jhou')).toBe('Hello, Jhou');
        });
    });
});
grails {
	awsAccounts=[ System.env.AWS_ACCOUNT_NUMBER ]

	awsAccountNames=[]
        awsAccountNames.put(System.env.AWS_ACCOUNT_NUMBER,"prod")
}
secret {
	accessId=System.env.AWS_KEY
	secretKey=System.env.AWS_SECRET
}
cloud {
	accountName="prod"
	publicResourceAccounts=[]
}

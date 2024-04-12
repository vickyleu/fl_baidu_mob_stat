#import "BaiduMobStatPlugin.h"
#import "BaiduMobStat.h"

@interface BaiduMobStatPlugin()
@property(nonatomic,strong)NSString* appkey;
@property(nonatomic,assign)BOOL isStarted;
@end

@implementation BaiduMobStatPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    FlutterMethodChannel *channel = [FlutterMethodChannel
                                     methodChannelWithName:@"fl_baidu_mob_stat"
                                     binaryMessenger:[registrar messenger]];
    BaiduMobStatPlugin *instance = [[BaiduMobStatPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    
    if ([call.method isEqualToString:@"init"]) {
       NSString* appKey =  call.arguments[@"appKey"];
       NSString* appChannel =  call.arguments[@"appChannel"];
       NSString* versionName =  call.arguments[@"versionName"];
       NSString* debuggable =  call.arguments[@"debuggable"];
        BaiduMobStat* stat =   [BaiduMobStat defaultStat];
        self.appkey = appKey;
       
        [stat setChannelId:appChannel];
        [stat setShortAppVersion:versionName];
        [stat setEnableDebugOn: [debuggable isEqual:@"true"]];
        [stat setAuthorizedState:NO];
        result(@(YES));
    } else if ([call.method isEqualToString:@"privilegeGranted"]) {
        BaiduMobStat* stat =   [BaiduMobStat defaultStat];
        [stat setAuthorizedState:YES];
        [stat setPlatformType:2];
        [stat startWithAppId:self.appkey];
        self.isStarted = YES;
        result(@(YES));
    } else if ([call.method isEqualToString:@"logEvent"] && self.isStarted ) {
        NSString *eventId = call.arguments[@"eventId"];
        NSDictionary *attributes = [self validArgument:call.arguments[@"attributes"]];
        [[BaiduMobStat defaultStat] logEvent:eventId attributes:attributes];
        result(@(YES));
    } else if ([call.method isEqualToString:@"logDurationEvent"] && self.isStarted ) {
        NSString *eventId = call.arguments[@"eventId"];
        NSString *eventLabel = call.arguments[@"label"];
        NSInteger duration = [call.arguments[@"duration"] integerValue];
        NSDictionary *attributes = [self validArgument:call.arguments[@"attributes"]];
        [[BaiduMobStat defaultStat] logEventWithDurationTime:eventId eventLabel:eventLabel durationTime:duration attributes:attributes];
        result(@(YES));
    } else if ([call.method isEqualToString:@"eventStart"] && self.isStarted ) {
        [[BaiduMobStat defaultStat] eventStart:call.arguments[@"eventId"] eventLabel:call.arguments[@"label"]];
        result(@(YES));
    } else if ([call.method isEqualToString:@"eventEnd"] && self.isStarted ) {
        NSString *eventId = call.arguments[@"eventId"];
        NSString *label = call.arguments[@"label"];
        NSDictionary *attributes = [self validArgument:call.arguments[@"attributes"]];
        [[BaiduMobStat defaultStat] eventEnd:eventId eventLabel:label attributes:attributes];
        result(@(YES));
    } else if ([call.method isEqualToString:@"pageStart"] && self.isStarted ) {
        [[BaiduMobStat defaultStat] pageviewStartWithName:call.arguments];
        result(@(YES));
    } else if ([call.method isEqualToString:@"pageEnd"] && self.isStarted ) {
        [[BaiduMobStat defaultStat] pageviewEndWithName:call.arguments];
        result(@(YES));
    } else {
        result(FlutterMethodNotImplemented);
    }
}

// dart侧可选参数未传或传nil，oc侧接收到的是NSNull
- (id)validArgument:(id)argument {
    return [argument isEqual:[NSNull null]] ? nil : argument;
}

@end

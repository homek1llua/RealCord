#!/usr/bin/env python3
"""Generate RealCord.xcodeproj/project.pbxproj with deterministic UUIDs."""

import hashlib, os, pathlib, uuid as uuid_mod

PROJECT_DIR = pathlib.Path(__file__).parent / "RealCord"
OUTPUT_DIR = pathlib.Path(__file__).parent / "RealCord.xcodeproj"

def uid(seed: str) -> str:
    return hashlib.md5(seed.encode()).hexdigest()[:24].upper()

def find_files(base: pathlib.Path, pattern: str, parent: str = "RealCord") -> list:
    files = []
    for f in sorted(base.rglob(pattern)):
        rel = str(f.relative_to(base.parent))
        files.append(rel)
    return files

swift_files = find_files(PROJECT_DIR, "*.swift")
xcassets = find_files(PROJECT_DIR, "*.xcassets")
asset_contents = [f for f in find_files(PROJECT_DIR, "Contents.json") if ".xcassets" in f]

GID = uid("root")

def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')

O = []
O.append('// !$*UTF8*$!\n')
O.append('{\n')

def kv(key, val, indent=1):
    t = '\t' * indent
    if isinstance(val, dict):
        if not val:
            O.append(f'{t}{key} = {{}};\n')
            return
        O.append(f'{t}{key} = {{\n')
        for k, v in val.items():
            kv(k, v, indent+1)
        O.append(f'{t}}};\n')
    elif isinstance(val, list):
        if not val:
            O.append(f'{t}{key} = (\n')
            O.append(f'{t});\n')
            return
        O.append(f'{t}{key} = (\n')
        for item in val:
            if isinstance(item, dict):
                O.append(f'{t}\t{{\n')
                for k, v in item.items():
                    kv(k, v, indent+2)
                O.append(f'{t}\t}},\n')
            else:
                O.append(f'{t}\t{item},\n')
        O.append(f'{t});\n')
    elif isinstance(val, bool):
        O.append(f'{t}{key} = {"YES" if val else "NO"};\n')
    elif isinstance(val, int):
        O.append(f'{t}{key} = {val};\n')
    else:
        O.append(f'{t}{key} = "{esc(val)}";\n')

sections = {}

B = {}  # PBXBuildFile
F = {}  # PBXFileReference
G = {}  # PBXGroup
P = {}  # PBXBuildPhase
T = {}  # Target
C = {}  # Config

# File references for all source files
src_refs = []
for sf in swift_files:
    ref = uid('ref_' + sf)
    F[ref] = {"isa": "PBXFileReference", "lastKnownFileType": "sourcecode.swift", "path": os.path.basename(sf), "sourceTree": "<group>"}
    B[uid('build_' + sf)] = {"isa": "PBXBuildFile", "fileRef": ref}
    src_refs.append(ref)

# File references for asset catalogs
asset_refs = []
for af in xcassets:
    ref = uid('ref_' + af)
    F[ref] = {"isa": "PBXFileReference", "lastKnownFileType": "folder.assetcatalog", "path": os.path.basename(af), "sourceTree": "<group>"}
    B[uid('build_' + af)] = {"isa": "PBXBuildFile", "fileRef": ref}
    asset_refs.append(ref)

# GoogleService-Info.plist
gs_ref = uid('ref_googleservice')
F[gs_ref] = {"isa": "PBXFileReference", "lastKnownFileType": "text.plist.xml", "path": "GoogleService-Info.plist", "sourceTree": "<group>"}
B[uid('build_googleservice')] = {"isa": "PBXBuildFile", "fileRef": gs_ref}

# Product reference
prod_ref = uid('product')
F[prod_ref] = {"isa": "PBXFileReference", "explicitFileType": "wrapper.application", "includeInIndex": 0, "path": "RealCord.app", "sourceTree": "BUILT_PRODUCTS_DIR"}

# Groups
main_group_id = uid('main_group')
products_group_id = uid('products_group')
G[GID] = {"isa": "PBXGroup", "children": [main_group_id, products_group_id], "sourceTree": "<group>"}
G[main_group_id] = {"isa": "PBXGroup", "children": src_refs + asset_refs + [gs_ref], "path": "RealCord", "sourceTree": "<group>"}
G[products_group_id] = {"isa": "PBXGroup", "children": [prod_ref], "name": "Products", "sourceTree": "<group>"}

# Build phases
src_phase_id = uid('sources_phase')
fw_phase_id = uid('frameworks_phase')
res_phase_id = uid('resources_phase')
P[src_phase_id] = {"isa": "PBXSourcesBuildPhase", "buildActionMask": 2147483647, "files": [{"fileRef": uid('ref_' + sf)} for sf in swift_files], "runOnlyForDeploymentPostprocessing": 0}
P[fw_phase_id] = {"isa": "PBXFrameworksBuildPhase", "buildActionMask": 2147483647, "files": [], "runOnlyForDeploymentPostprocessing": 0}
P[res_phase_id] = {"isa": "PBXResourcesBuildPhase", "buildActionMask": 2147483647, "files": [{"fileRef": gs_ref}], "runOnlyForDeploymentPostprocessing": 0}

# Target
target_id = uid('target')
T[target_id] = {
    "isa": "PBXNativeTarget",
    "buildConfigurationList": uid('target_config_list'),
    "buildPhases": [src_phase_id, fw_phase_id, res_phase_id],
    "buildRules": [],
    "dependencies": [],
    "name": "RealCord",
    "productName": "RealCord",
    "productReference": prod_ref,
    "productType": "com.apple.product-type.application",
}

# Configs
proj_debug_id = uid('proj_debug')
proj_release_id = uid('proj_release')
target_debug_id = uid('target_debug')
target_release_id = uid('target_release')
proj_config_list_id = uid('proj_config_list')
target_config_list_id = uid('target_config_list')

base_debug = {
    "ALWAYS_SEARCH_USER_PATHS": "NO",
    "CLANG_ENABLE_MODULES": "YES",
    "DEBUG_INFORMATION_FORMAT": "dwarf",
    "ENABLE_STRICT_OBJC_MSGSEND": "YES",
    "ENABLE_TESTABILITY": "YES",
    "GCC_DYNAMIC_NO_PIC": "NO",
    "GCC_OPTIMIZATION_LEVEL": "0",
    "IPHONEOS_DEPLOYMENT_TARGET": "15.0",
    "MTL_ENABLE_DEBUG_INFO": "INCLUDE_SOURCE",
    "ONLY_ACTIVE_ARCH": "YES",
    "SDKROOT": "iphoneos",
    "SWIFT_ACTIVE_COMPILATION_CONDITIONS": "DEBUG",
    "SWIFT_OPTIMIZATION_LEVEL": "-Onone",
}
base_release = {
    "ALWAYS_SEARCH_USER_PATHS": "NO",
    "CLANG_ENABLE_MODULES": "YES",
    "DEBUG_INFORMATION_FORMAT": "dwarf-with-dsym",
    "ENABLE_NS_ASSERTIONS": "NO",
    "ENABLE_STRICT_OBJC_MSGSEND": "YES",
    "GCC_OPTIMIZATION_LEVEL": "s",
    "IPHONEOS_DEPLOYMENT_TARGET": "15.0",
    "MTL_ENABLE_DEBUG_INFO": "NO",
    "SDKROOT": "iphoneos",
    "SWIFT_COMPILATION_MODE": "wholemodule",
    "SWIFT_OPTIMIZATION_LEVEL": "-O",
    "VALIDATE_PRODUCT": "YES",
}

target_base = {
    "ASSETCATALOG_COMPILER_APPICON_NAME": "AppIcon",
    "CODE_SIGN_STYLE": "Automatic",
    "CURRENT_PROJECT_VERSION": "1",
    "GENERATE_INFOPLIST_FILE": "YES",
    "INFOPLIST_FILE": "RealCord/Info.plist",
    "INFOPLIST_KEY_UIApplicationSceneManifest_Generation": "YES",
    "INFOPLIST_KEY_UIApplicationSupportsIndirectInputEvents": "YES",
    "INFOPLIST_KEY_UILaunchScreen_Generation": "YES",
    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPad": "UIInterfaceOrientationPortrait_UIInterfaceOrientationLandscapeLeft_UIInterfaceOrientationLandscapeRight",
    "INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone": "UIInterfaceOrientationPortrait_UIInterfaceOrientationLandscapeLeft_UIInterfaceOrientationLandscapeRight",
    "LD_RUNPATH_SEARCH_PATHS": ["$(inherited)", "@executable_path/Frameworks"],
    "MARKETING_VERSION": "1.0",
    "PRODUCT_BUNDLE_IDENTIFIER": "com.realcord",
    "PRODUCT_NAME": "$(TARGET_NAME)",
    "SWIFT_VERSION": "5.0",
    "TARGETED_DEVICE_FAMILY": "1,2",
}

C[proj_debug_id] = {"isa": "XCBuildConfiguration", "buildSettings": base_debug, "name": "Debug"}
C[proj_release_id] = {"isa": "XCBuildConfiguration", "buildSettings": base_release, "name": "Release"}
C[target_debug_id] = {"isa": "XCBuildConfiguration", "buildSettings": {**target_base}, "name": "Debug"}
C[target_release_id] = {"isa": "XCBuildConfiguration", "buildSettings": {**target_base}, "name": "Release"}
C[proj_config_list_id] = {"isa": "XCConfigurationList", "buildConfigurations": [proj_debug_id, proj_release_id], "defaultConfigurationIsVisible": 0, "defaultConfigurationName": "Release"}
C[target_config_list_id] = {"isa": "XCConfigurationList", "buildConfigurations": [target_debug_id, target_release_id], "defaultConfigurationIsVisible": 0, "defaultConfigurationName": "Release"}

# Project
proj_id = uid('project')
C[proj_id] = {
    "isa": "PBXProject",
    "attributes": {"BuildIndependentTargetsInParallel": 1, "LastSwiftUpdateCheck": 1500, "LastUpgradeCheck": 1500},
    "buildConfigurationList": proj_config_list_id,
    "compatibilityVersion": "Xcode 14.0",
    "developmentRegion": "en",
    "hasScannedForEncodings": 0,
    "knownRegions": ["en", "Base"],
    "mainGroup": GID,
    "productRefGroup": products_group_id,
    "projectDirPath": "",
    "projectRoot": "",
    "targets": [target_id],
}

# Assemble
all_objects = {}
for d in [B, F, G, P, T, C]:
    all_objects.update(d)

sections = {
    "archiveVersion": 1,
    "classes": {},
    "objectVersion": 56,
    "objects": all_objects,
    "rootObject": GID,
}

kv("archiveVersion", 1, 1)
kv("classes", {}, 1)
kv("objectVersion", 56, 1)
kv("objects", all_objects, 1)
kv("rootObject", GID, 1)

O.append('}\n')

OUTPUT_DIR.mkdir(exist_ok=True)
with open(OUTPUT_DIR / "project.pbxproj", "w") as f:
    f.write(''.join(O))

print(f"Generated: {OUTPUT_DIR / 'project.pbxproj'}")
print(f"Swift files: {len(swift_files)}")
print(f"Asset catalogs: {len(xcassets)}")

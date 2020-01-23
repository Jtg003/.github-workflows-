import pytest

from collections import defaultdict

from Pegasus.dax4.mixins import MetadataMixin
from Pegasus.dax4.mixins import _Hook
from Pegasus.dax4.mixins import EventType
from Pegasus.dax4.mixins import _ShellHook
from Pegasus.dax4.mixins import HookMixin
from Pegasus.dax4.mixins import Namespace
from Pegasus.dax4.mixins import ProfileMixin
from Pegasus.dax4.errors import DuplicateError
from Pegasus.dax4.errors import NotFoundError


@pytest.fixture(scope="function")
def md_mixin_obj():
    def _metadata_mixin_obj():
        class MetadataMixinObj(MetadataMixin):
            def __init__(self):
                self.metadata = dict()

        return MetadataMixinObj()

    return _metadata_mixin_obj()


class TestMetadataMixin:
    def test_add_metadata(self, md_mixin_obj):
        md_mixin_obj.add_metadata("key", "value")
        assert md_mixin_obj.metadata["key"] == "value"

    def test_add_duplicate_metadata(self, md_mixin_obj):
        md_mixin_obj.add_metadata("key", "value")
        with pytest.raises(DuplicateError):
            md_mixin_obj.add_metadata("key", "value")

    def test_chaining(self, md_mixin_obj):
        (md_mixin_obj.add_metadata("key1", "value").add_metadata("key2", "value"))

        assert len(md_mixin_obj.metadata) == 2
        assert id(md_mixin_obj) == id(md_mixin_obj.add_metadata("key3", "value"))


class Test_Hook:
    def test_valid_hook(self):
        _Hook(EventType.START)

    def test_invalid_hook(self):
        with pytest.raises(ValueError):
            _Hook("123")


class Test_ShellHook:
    def test_valid_shell_hook(self):
        _ShellHook(EventType.START, "some command")

    def test_invalid_shell_hook(self):
        with pytest.raises(ValueError):
            _ShellHook("123", "some command")

    def test_tojson(self):
        hook = _ShellHook(EventType.START, "some command")
        assert hook.__json__() == {"_on": "start", "cmd": "some command"}


@pytest.fixture(scope="function")
def hook_mixin_obj():
    def _hook_mixin_obj():
        class HookMixinObj(HookMixin):
            def __init__(self):
                self.hooks = defaultdict(list)

        return HookMixinObj()

    return _hook_mixin_obj()


class TestHookMixin:
    def test_add_shell_hook(self, hook_mixin_obj):
        cmd = "some command"
        hook_mixin_obj.add_shell_hook(EventType.START, cmd)
        assert "shell" in hook_mixin_obj.hooks

        added_shell_hook = hook_mixin_obj.hooks["shell"][0]

        assert added_shell_hook.on == EventType.START.value
        assert added_shell_hook.cmd == cmd

    def test_add_invalid_shell_hook(self, hook_mixin_obj):
        with pytest.raises(ValueError):
            hook_mixin_obj.add_shell_hook("123", "some command")

    def test_chaining(self, hook_mixin_obj):
        (
            hook_mixin_obj.add_shell_hook(
                EventType.START, "some command"
            ).add_shell_hook(EventType.START, "another command")
        )

        assert "shell" in hook_mixin_obj.hooks

        added_shell_hook_1 = hook_mixin_obj.hooks["shell"][0]
        assert added_shell_hook_1.on == EventType.START.value
        assert added_shell_hook_1.cmd == "some command"

        added_shell_hook_2 = hook_mixin_obj.hooks["shell"][1]
        assert added_shell_hook_2.on == EventType.START.value
        assert added_shell_hook_2.cmd == "another command"

        assert id(hook_mixin_obj) == id(
            hook_mixin_obj.add_shell_hook(EventType.START, "some command")
        )


@pytest.fixture(scope="function")
def profile_mixin_obj():
    def _profile_mixin_obj():
        class ProfileMixinObj(ProfileMixin):
            def __init__(self):
                self.profiles = defaultdict(dict)

        return ProfileMixinObj()

    return _profile_mixin_obj()


class TestProfileMixin:
    def test_add_valid_profile(self, profile_mixin_obj):
        profile_mixin_obj.add_profile(Namespace.ENV, "JAVA_HOME", "/usr/bin/java")

        assert Namespace.ENV.value in profile_mixin_obj.profiles
        assert "JAVA_HOME" in profile_mixin_obj.profiles[Namespace.ENV.value]
        assert (
            profile_mixin_obj.profiles[Namespace.ENV.value]["JAVA_HOME"]
            == "/usr/bin/java"
        )

    def test_add_invalid_profile(self, profile_mixin_obj):
        with pytest.raises(ValueError):
            profile_mixin_obj.add_profile("namespace", "key", "value")

    def test_chaining(self, profile_mixin_obj):
        (
            profile_mixin_obj.add_profile(Namespace.ENV, "key", "value").add_profile(
                Namespace.GLOBUS, "key", "value"
            )
        )

        assert len(profile_mixin_obj.profiles) == 2
        assert id(profile_mixin_obj.add_profile(Namespace.ENV, "key2", "value")) == id(
            profile_mixin_obj
        )
